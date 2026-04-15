# SwiftDeal - Stripe Payment Integration Setup

## Overview
This setup enables automatic 90/10 payment splitting:
- **Passengers** can pay online using saved cards
- **Drivers** receive 90% of fare automatically after trip completion
- **SwiftDeal** retains 10% as platform fee

## Files Structure
```
SwiftDeal/
├── www/
│   ├── passenger.html    # Passenger app (main site)
│   ├── driver.html       # Driver app
│   ├── host.html         # Admin dashboard
│   └── index.html        # Copy of passenger.html
├── functions/
│   ├── index.js          # Cloud Functions for Stripe
│   └── package.json      # Functions dependencies
├── firebase.json         # Hosting configuration
└── .firebaserc           # Project targets
```

## Setup Steps

### 1. Configure Stripe Keys

#### In Firebase Functions:
```bash
firebase functions:config:set stripe.secret_key="sk_live_YOUR_SECRET_KEY"
firebase functions:config:set stripe.webhook_secret="whsec_YOUR_WEBHOOK_SECRET"
```

#### In passenger.html (line ~1115):
```javascript
const STRIPE_PUBLISHABLE_KEY = 'pk_live_YOUR_PUBLISHABLE_KEY';
```

### 2. Deploy Cloud Functions
```bash
cd functions
npm install
cd ..
firebase deploy --only functions
```

### 3. Deploy Hosting
```bash
firebase deploy --only hosting
```

### 4. Set Up Stripe Webhook
1. Go to Stripe Dashboard → Developers → Webhooks
2. Add endpoint: `https://us-central1-swiftdeal-3cee9.cloudfunctions.net/stripeWebhook`
3. Select events:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
   - `account.updated`
4. Copy the webhook secret and set it in Firebase config

## Payment Flow

### For Passengers (Online Payment):
1. Passenger selects "Pay Online" when booking
2. Passenger confirms driver
3. Payment is **held** (not charged) via Stripe
4. Trip is completed
5. Payment is **captured** and driver receives 90%

### For Drivers:
1. Driver must connect Stripe account (Menu → Connect Stripe)
2. Complete Stripe Express onboarding
3. Accept online payment requests
4. Complete trip → Automatic payout!

### For Cash Payments:
- Works as before
- Driver collects cash from passenger
- Commission (10%) is added to driver's balance due

## Database Structure

### New Fields in `/requests/{requestId}`:
```json
{
  "paymentMethod": "online" | "cash",
  "stripePaymentIntentId": "pi_xxx",
  "paymentStatus": "held" | "captured" | "cancelled",
  "driverPayout": 900,
  "serviceFee": 100
}
```

### New Fields in `/driverAccounts/{driverId}`:
```json
{
  "stripeAccountId": "acct_xxx",
  "stripeAccountStatus": "active" | "pending",
  "stripeChargesEnabled": true,
  "stripePayoutsEnabled": true
}
```

### New Collections:
- `/payoutLogs` - Record of all driver payouts
- `/failedPayouts` - Failed payout attempts for debugging

## Stripe Connect Countries
By default, drivers are onboarded as Jamaica (JM) accounts.
To change, modify line 205 in `functions/index.js`:
```javascript
country: 'JM', // Change to your country code
```

## Testing
1. Use Stripe test keys first (`pk_test_...`, `sk_test_...`)
2. Test card number: `4242 4242 4242 4242`
3. Any future expiry and CVC

## Troubleshooting

### Driver can't accept online payments
- Check if driver has connected Stripe account
- Verify `stripeChargesEnabled` and `stripePayoutsEnabled` are true

### Payment fails
- Check `card-errors` element for Stripe errors
- Check browser console for details
- Verify Stripe keys are correct

### Payout not received
- Check `/failedPayouts` in Firebase
- Verify driver's Stripe account is fully verified
- Check Stripe Dashboard for transfer status

## Support
For issues, contact: swiftdealrides@gmail.com
