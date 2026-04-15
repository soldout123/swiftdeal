/**
 * SwiftDeal - Firebase Cloud Functions for Stripe Payments (v2)
 * Updated February 4, 2026
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require('firebase-functions/params');
const admin = require('firebase-admin');

admin.initializeApp();

// Access the STRIPE_SECRET you set via: firebase functions:secrets:set STRIPE_SECRET
const stripeSecret = defineSecret('STRIPE_SECRET');

const PLATFORM_FEE_PERCENT = 0.10; // SwiftDeal keeps 10%
const DRIVER_PAYOUT_PERCENT = 0.90; // Driver gets 90%

/**
 * 1. PASSENGER: Create a PaymentIntent with manual capture
 */
exports.createPaymentIntent = onCall({ secrets: [stripeSecret] }, async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'User must be logged in');
    }

    const stripe = require('stripe')(stripeSecret.value());
    const { amount, currency, paymentMethodId, requestId, driverId, passengerId, passengerName } = request.data;

    if (!amount || !currency || !paymentMethodId || !requestId) {
        throw new HttpsError('invalid-argument', 'Missing required fields');
    }

    try {
        // Create PaymentIntent with manual capture (funds hold)
        const paymentIntent = await stripe.paymentIntents.create({
            amount: Math.round(amount * 100), // Convert to cents
            currency: currency.toLowerCase(),
            payment_method: paymentMethodId,
            confirmation_method: 'manual',
            capture_method: 'manual', 
            confirm: true,
            metadata: { requestId, driverId: driverId || '', passengerId: passengerId || request.auth.uid },
            return_url: `https://swiftdeal-3cee9.web.app/payment-complete?requestId=${requestId}`
        });

        // Save to Database
        await admin.database().ref(`requests/${requestId}`).update({
            stripePaymentIntentId: paymentIntent.id,
            paymentStatus: paymentIntent.status,
            amountHeld: amount
        });

        return { success: true, paymentIntentId: paymentIntent.id, clientSecret: paymentIntent.client_secret };
    } catch (error) {
        throw new HttpsError('internal', error.message);
    }
});

/**
 * 2. DRIVER: Capture payment and transfer 90% to driver
 */
exports.finishTripAndPayout = onCall({ secrets: [stripeSecret] }, async (request) => {
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'User must be logged in');
    }

    const stripe = require('stripe')(stripeSecret.value());
    const { requestId, driverId } = request.data;

    try {
        const requestSnap = await admin.database().ref(`requests/${requestId}`).once('value');
        const rideRequest = requestSnap.val();

        if (!rideRequest?.stripePaymentIntentId) {
            throw new HttpsError('failed-precondition', 'No PaymentIntent found');
        }

        const driverSnap = await admin.database().ref(`driverAccounts/${driverId}`).once('value');
        const driver = driverSnap.val();

        if (!driver?.stripeAccountId) {
            throw new HttpsError('failed-precondition', 'Driver Stripe account not connected');
        }

        // A. Capture full amount from passenger
        const paymentIntent = await stripe.paymentIntents.capture(rideRequest.stripePaymentIntentId);

        // B. Calculate 90% split
        const totalAmountCents = paymentIntent.amount;
        const driverAmountCents = Math.floor(totalAmountCents * DRIVER_PAYOUT_PERCENT);

        // C. Transfer to Driver
        const transfer = await stripe.transfers.create({
            amount: driverAmountCents,
            currency: paymentIntent.currency,
            destination: driver.stripeAccountId,
        });

        // D. Update Database
        await admin.database().ref(`requests/${requestId}`).update({
            paymentStatus: 'captured',
            stripeTransferId: transfer.id,
            driverPayout: driverAmountCents / 100
        });

        return { success: true, driverPayout: driverAmountCents / 100 };
    } catch (error) {
        throw new HttpsError('internal', error.message);
    }
});

/**
 * 3. ONBOARDING: Create Stripe Connect link for drivers
 */
exports.createStripeConnectAccount = onCall({ secrets: [stripeSecret] }, async (request) => {
    const stripe = require('stripe')(stripeSecret.value());
    const { driverId, email } = request.data;

    try {
        const account = await stripe.accounts.create({
            type: 'express',
            country: 'JM',
            email: email,
            capabilities: { card_payments: { requested: true }, transfers: { requested: true } },
        });

        await admin.database().ref(`driverAccounts/${driverId}`).update({
            stripeAccountId: account.id,
            stripeAccountStatus: 'pending'
        });

        const accountLink = await stripe.accountLinks.create({
            account: account.id,
            refresh_url: `https://swiftdeal-driver.firebaseapp.com/?stripe_refresh=true`,
            return_url: `https://swiftdeal-driver.firebaseapp.com/?stripe_success=true`,
            type: 'account_onboarding'
        });

        return { url: accountLink.url };
    } catch (error) {
        throw new HttpsError('internal', error.message);
    }
});
