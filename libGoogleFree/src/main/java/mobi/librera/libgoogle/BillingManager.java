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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BillingManager {
    static BillingManager INSTANCE = new BillingManager();
    private final List<ProductDetails> productDetails = new CopyOnWriteArrayList<>();
    private final List<Purchase> activeSubscriptions = new CopyOnWriteArrayList<>();
    private static final String SUBSCRIPTION_PRODUCT_ID = "subscription_month_id";
    private BillingClient billingClient;

    private BillingManager() {

    }

    public static BillingManager get() {
        return INSTANCE;
    }

    public void init(Context context) {
        setupBillingClient(context);
    }

    static String TAG = "BillingManager";

    private void acknowledgePurchase(Purchase purchase) {
        if (!purchase.isAcknowledged()) {
            AcknowledgePurchaseParams params =
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

            billingClient.acknowledgePurchase(params, acknowledgeResult -> {
                if (acknowledgeResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Purchase acknowledged successfully");
                } else {
                    Log.e(TAG, "Acknowledgement failed: " + acknowledgeResult.getDebugMessage());
                }
            });
        }
    }

    private void setupBillingClient(Context context) {

        PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
            Log.d(TAG, "Purchase: purchases" + purchases.size());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    Log.d(TAG, "Purchase: isAcknowledged" + purchase.isAcknowledged());
                    Log.d(TAG, "Purchase: isSuspended" + purchase.isSuspended());
                    Log.d(TAG, "Purchase: isAutoRenewing" + purchase.isAutoRenewing());
                    Log.d(TAG, "Purchase: getPurchaseToken" + purchase.getPurchaseToken());
                    acknowledgePurchase(purchase);
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {

            } else {

            }
        };

        billingClient = BillingClient.newBuilder(context)
                                     .setListener(purchasesUpdatedListener)
                                     .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                                                                                   .enableOneTimeProducts()
                                                                                   .enablePrepaidPlans()
                                                                                   .build())
                                     .enableAutoServiceReconnection()
                                     .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing Client Ready");
                    loadAllSubscriptions();
                } else {
                    Log.e("BillingManager", "Setup failed: " + billingResult.getDebugMessage());
                }
            }

            @Override public void onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected – will retry");
            }
        });
    }

    public void launchSubscription(Activity a) {

        ProductDetails subDetails = productDetails.get(0);

        ProductDetails.SubscriptionOfferDetails selectedOffer = null;

        if (subDetails.getSubscriptionOfferDetails() != null && !subDetails.getSubscriptionOfferDetails().isEmpty()) {
            selectedOffer = subDetails.getSubscriptionOfferDetails().get(0);
        }

        String offerToken = selectedOffer.getOfferToken();
        Log.e("BillingManager", "offerToken" + offerToken);

        BillingFlowParams.ProductDetailsParams productDetailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                                                      .setProductDetails(productDetails.get(0))
                                                      .setOfferToken(offerToken)
                                                      .build();

        BillingFlowParams params = BillingFlowParams.newBuilder().setProductDetailsParamsList(List.of(
                productDetailsParams)).build();

        BillingResult billingResult = billingClient.launchBillingFlow(a, params);
        Log.e("BillingManager", "getResponseCode" + billingResult.getResponseCode());
        //BillingClient.BillingResponseCode.OK

    }

    public boolean isHasSubscription() {
        if (productDetails == null || productDetails.isEmpty()) {
            return false;
        }

        ProductDetails subDetails = productDetails.get(0);
        return subDetails.getSubscriptionOfferDetails() != null && !subDetails.getSubscriptionOfferDetails().isEmpty();
    }

    public String getFormattedPrice() {
        try {
            return productDetails.get(0)
                                 .getSubscriptionOfferDetails()
                                 .get(0)
                                 .getPricingPhases()
                                 .getPricingPhaseList()
                                 .get(0)
                                 .getFormattedPrice();
        } catch (Exception e) {
            return "1$";
        }
    }

    // Load ALL subscription plans (including all base plans and offers)
    public void loadAllSubscriptions() {

        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder()
                                                         .setProductId(SUBSCRIPTION_PRODUCT_ID)
                                                         .setProductType(BillingClient.ProductType.SUBS)
                                                         .build());

        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(productList).build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {

                productDetails.clear();
                productDetails.addAll(productDetailsList.getProductDetailsList());

                for (ProductDetails details : productDetails) {
                    if (details.getProductType().equals(BillingClient.ProductType.SUBS)) {
                        details.getSubscriptionOfferDetails()
                               .get(0)
                               .getPricingPhases()
                               .getPricingPhaseList()
                               .get(0)
                               .getFormattedPrice();
                    }
                }
            } else {
                Log.e("BillingManager", "Query failed: " + billingResult.getDebugMessage());
            }
        });
    }

    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}