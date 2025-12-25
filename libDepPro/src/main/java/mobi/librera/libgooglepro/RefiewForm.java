package mobi.librera.libgooglepro;

import android.app.Activity;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;

public class RefiewForm {

    public static void show(Activity a, Runnable onFisnish) {
        ReviewManager manager = ReviewManagerFactory.create(a);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {

            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
                if (reviewInfo == null) return;
                Task<Void> flow = manager.launchReviewFlow(a, reviewInfo);
                flow.addOnCompleteListener(task2 -> {
                    onFisnish.run();
                });

            } else {
                onFisnish.run();
            }
        });
    }
}
