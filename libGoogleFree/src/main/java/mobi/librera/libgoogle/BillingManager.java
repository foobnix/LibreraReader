package mobi.librera.libgoogle;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BillingManager {

    private BillingClient billingClient;
    private final Context context;

    // Callbacks
    public interface OnSubscriptionsLoadedListener {
        void onSubscriptionsLoaded(List<ProductDetails> productDetailsList);
    }

    public interface OnPurchaseFinishedListener {
        void onPurchaseSuccess(String productId);

        void onPurchaseFailed(String errorMessage);
    }

    private OnSubscriptionsLoadedListener loadedListener;
    private OnPurchaseFinishedListener purchaseListener;

    public BillingManager(Context context) {
        this.context = context.getApplicationContext();
        setupBillingClient();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(context)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()  // Required even for subscriptions only
                                .enablePrepaidPlans()
                                .build()
                )
                .enableAutoServiceReconnection()
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing Client Ready");
                    // Optionally auto-query can go here if needed
                } else {
                    Log.e("BillingManager", "Setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected – will retry");
            }
        });
    }

    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (purchaseListener != null) {
                purchaseListener.onPurchaseFailed("User canceled the purchase");
            }
        } else {
            if (purchaseListener != null) {
                purchaseListener.onPurchaseFailed(billingResult.getDebugMessage());
            }
        }
    };

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            }

            // Notify success – you can send any product ID from purchase.getProducts()
            if (purchaseListener != null && !purchase.getProducts().isEmpty()) {
                purchaseListener.onPurchaseSuccess(purchase.getProducts().get(0));
            }
        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        billingClient.acknowledgePurchase(params, acknowledgeResult -> {
            // Acknowledged
        });
    }

    // Load ALL subscription plans (including all base plans and offers)
    public void loadAllSubscriptions(OnSubscriptionsLoadedListener listener) {
        this.loadedListener = listener;

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId("subscription_month_id")   // Your subscription product IDs
                .setProductType(BillingClient.ProductType.SUBS)
                .build());

        // Add more subscriptions here

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                List<ProductDetails> subsOnly = new ArrayList<>();
                for (ProductDetails details : productDetailsList.getProductDetailsList()) {
                    if (details.getProductType().equals(BillingClient.ProductType.SUBS)) {
                        subsOnly.add(details);
                    }
                }
                if (loadedListener != null) {
                    loadedListener.onSubscriptionsLoaded(subsOnly);
                }
            } else {
                Log.e("BillingManager", "Query failed: " + billingResult.getDebugMessage());
            }
        });
    }

    // Launch subscription purchase when user clicks "Subscribe"
    public void launchSubscriptionFlow(Activity activity, ProductDetails productDetails,
                                       OnPurchaseFinishedListener listener) {
        this.purchaseListener = listener;

        // Pick the offer (usually first one, or let user choose)
        List<ProductDetails.SubscriptionOfferDetails> offerDetails =
                productDetails.getSubscriptionOfferDetails();

        if (offerDetails == null || offerDetails.isEmpty()) {
            Log.e("BillingManager", "No offers found for " + productDetails.getProductId());
            return;
        }

        String offerToken = offerDetails.get(0).getOfferToken(); // or show UI to pick

        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build();

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(params))
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.e("BillingManager", "Launch failed: " + result.getDebugMessage());
        }
    }

    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}