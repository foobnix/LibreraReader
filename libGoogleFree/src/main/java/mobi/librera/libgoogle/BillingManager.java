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
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;

public class BillingManager {

    private BillingClient billingClient;
    List<ProductDetails> productDetails;


    static BillingManager INSTANCE = new BillingManager();

    public void init(Context context) {
        setupBillingClient(context);
    }

    public static BillingManager get() {
        return INSTANCE;
    }

    private void setupBillingClient(Context context) {
        billingClient = BillingClient.newBuilder(context).setListener(purchasesUpdatedListener).enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts()  // Required even for subscriptions only
                .enablePrepaidPlans().build()).enableAutoServiceReconnection().build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing Client Ready");
                    // Optionally auto-query can go here if needed

                    loadAllSubscriptions();
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

        } else {

        }
    };

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            }


        }
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();

        billingClient.acknowledgePurchase(params, acknowledgeResult -> {
            // Acknowledged
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


        BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails.get(0)) // The ProductDetails object
                .setOfferToken(offerToken).build();

        BillingFlowParams params = BillingFlowParams.newBuilder().setProductDetailsParamsList(List.of(productDetailsParams)).build();

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
            return String.valueOf(productDetails.get(0).getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice());
        } catch (Exception e) {
            return "1$";
        }
    }


    // Load ALL subscription plans (including all base plans and offers)
    public void loadAllSubscriptions() {


        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(QueryProductDetailsParams.Product.newBuilder().setProductId("subscription_month_id").setProductType(BillingClient.ProductType.SUBS).build());


        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder().setProductList(productList).build();

        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {

            Log.d("BillingManager", "RES");
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                productDetails = productDetailsList.getProductDetailsList();
                for (ProductDetails details : productDetails) {
                    if (details.getProductType().equals(BillingClient.ProductType.SUBS)) {
                        Log.d("BillingManager", details.getDescription());
                        Log.d("BillingManager", details.getProductId());
                        Log.d("BillingManager", details.getName());

                        Log.d("BillingManager", String.valueOf(details.getSubscriptionOfferDetails().get(0).getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice()));
                    }
                }
            } else {
                Log.e("BillingManager", "Query failed: " + billingResult.getDebugMessage());
            }
        });

        QueryPurchasesParams queryPurchasesParams = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build();

        billingClient.queryPurchasesAsync(queryPurchasesParams, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                Log.d("BillingManager", "Purchases BillingResult:" + billingResult.getResponseCode());
                Log.d("BillingManager", "Purchases BillingResult list:" + list);
            }
        });
    }

    public void destroy() {
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }
}