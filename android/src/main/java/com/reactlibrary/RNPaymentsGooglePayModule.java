
package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Token;

import java.util.Arrays;

public class RNPaymentsGooglePayModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 42;

  private String stripePublishableKey;
  private PaymentsClient paymentsClient = null;
  private Promise requestPaymentPromise;

  private int environment;

  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
        System.out.println(requestPaymentPromise);
        if (requestPaymentPromise != null) {
          if (resultCode == Activity.RESULT_OK) {
            PaymentData paymentData = PaymentData.getFromIntent(intent);
            // You can get some data on the user's card, such as the brand and last 4 digits
            CardInfo info = paymentData.getCardInfo();
            // You can also pull the user address from the PaymentData object.
            UserAddress address = paymentData.getShippingAddress();
            // This is the raw JSON string version of your Stripe token.
            String rawToken = paymentData.getPaymentMethodToken().getToken();
            // Now that you have a Stripe token object, charge that by using the id
            Token stripeToken = Token.fromString(rawToken);
            if (stripeToken != null) {
              // This chargeToken function is a call to your own server, which should then connect
              // to Stripe's API to finish the charge.
              //chargeToken(stripeToken.getId());
              requestPaymentPromise.resolve(stripeToken.getId());
            } else {
              requestPaymentPromise.reject("PAYMENT_ERROR", "An error occurred");
            }
          } else if (resultCode == Activity.RESULT_CANCELED) {
            requestPaymentPromise.reject("PAYMENT_CANCELLED", "Payment has been cancelled");
          } else if (resultCode == AutoResolveHelper.RESULT_ERROR) {
            Status status = AutoResolveHelper.getStatusFromIntent(intent);
            requestPaymentPromise.reject("STRIPE_ERROR", status.getStatusMessage());
          }
          requestPaymentPromise = null;
        }
      }
    }
  };

  public RNPaymentsGooglePayModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    this.reactContext.addActivityEventListener(mActivityEventListener);
  }

  @Override
  public String getName() {
    return "RNPaymentsGooglePay";
  }

  private boolean setKey(String key) {
    if (key.contains("pk_test")) {
      this.environment = WalletConstants.ENVIRONMENT_TEST;
    } else if (key.contains("pk_live")) {
      this.environment = WalletConstants.ENVIRONMENT_PRODUCTION;
    } else {
      return false;
    }
    this.stripePublishableKey = key;
    return true;
  }

  private boolean isInitialized() {
    return this.environment != 0 && this.stripePublishableKey.length() > 0 && this.paymentsClient != null;
  }

  private PaymentMethodTokenizationParameters createTokenisationParameters() {
    return PaymentMethodTokenizationParameters.newBuilder()
            .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
            .addParameter("gateway", "stripe")
            .addParameter("stripe:publishableKey", this.stripePublishableKey)
            .addParameter("stripe:version", "5.1.0")
            .build();
  }

  private PaymentDataRequest createPaymentDataRequest(String totalPrice, String currency) {
    PaymentDataRequest.Builder request =
            PaymentDataRequest.newBuilder()
                    .setTransactionInfo(
                            TransactionInfo.newBuilder()
                                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                                    .setTotalPrice(totalPrice)
                                    .setCurrencyCode(currency)
                                    .build())
                    .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                    .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                    .setCardRequirements(
                            CardRequirements.newBuilder()
                                    .addAllowedCardNetworks(Arrays.asList(
                                            WalletConstants.CARD_NETWORK_AMEX,
                                            WalletConstants.CARD_NETWORK_DISCOVER,
                                            WalletConstants.CARD_NETWORK_VISA,
                                            WalletConstants.CARD_NETWORK_MASTERCARD))
                                    .build());

    request.setPaymentMethodTokenizationParameters(this.createTokenisationParameters());
    return request.build();
  }

  @ReactMethod
  public void initialize(ReadableMap initializeData, Promise promise) {
    if (this.isInitialized()) {
      promise.resolve(true);
    } else {
      String key = initializeData.getString("key");
      if (this.setKey(key)) {
        this.paymentsClient = Wallet.getPaymentsClient(
                this.reactContext,
                new Wallet.WalletOptions.Builder().setEnvironment(this.environment).build()
        );
        promise.resolve(true);
      } else {
        promise.reject("INVALID_KEY", "Invalid Stripe key");
      }
    }
  }

  @ReactMethod
  public void isReadyToPay(final Promise promise) {
    IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
            .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
            .build();
    if (this.isInitialized()) {
      Task<Boolean> task = this.paymentsClient.isReadyToPay(request);
      task.addOnCompleteListener(new OnCompleteListener<Boolean>() {
        public void onComplete(Task<Boolean> task) {
          try {
            boolean result = task.getResult(ApiException.class);
            if (result) {
              promise.resolve(true);
            } else {
              promise.reject("IS_NOT_READY", "Google Pay is not ready to make payments");
            }
          } catch (ApiException exception) {
            promise.reject("IS_NOT_READY", exception.getMessage());
          }
        }
      });
    } else {
      promise.reject("NOT_INITIALIZED", "Google Pay is not initialized");
    }
  }

  @ReactMethod
  public void requestPayment(String totalPrice, String currency, final Promise promise) {
    if (this.isInitialized()) {
      PaymentDataRequest request = this.createPaymentDataRequest(totalPrice, currency);
      Activity currentActivity = getCurrentActivity();
      if (currentActivity == null) {
        promise.reject("ACTIVITY_DOES_NOT_EXISTS", "Activity doesn't exist");
        return;
      }
      this.requestPaymentPromise = promise;
      AutoResolveHelper.resolveTask(
              this.paymentsClient.loadPaymentData(request),
              currentActivity,
              LOAD_PAYMENT_DATA_REQUEST_CODE
      );
    } else {
      promise.reject("NOT_INITIALIZED", "Google Pay is not initialized");
    }
  }
}