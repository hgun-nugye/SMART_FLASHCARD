package ntu.nguyenthithanhhuong.smartflashcard.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ntu.nguyenthithanhhuong.smartflashcard.R;
import ntu.nguyenthithanhhuong.smartflashcard.login.ChoiceLoginActivity;

public class FlashcardWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Chạy vòng lặp để cập nhật cho toàn bộ các Widget đang hiển thị trên màn hình chính
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_flashcard);

        // 1. Giao diện tạm thời trong lúc chờ hệ thống tải dữ liệu từ Firestore
        views.setTextViewText(R.id.widgetWord, "Loading...");
        views.setTextViewText(R.id.widgetMeaning, "Fetching your flashcards...");
        appWidgetManager.updateAppWidget(appWidgetId, views);

        // 2. Cấu hình sự kiện Phát sóng (Broadcast) cho nút "Đổi từ" (btnRefreshWidget)
        Intent refreshIntent = new Intent(context, FlashcardWidgetProvider.class);
        refreshIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{appWidgetId});

        PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId, // Đảm bảo ID duy nhất cho từng widget riêng biệt
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.btnRefreshWidget, refreshPendingIntent);

        // 3. Kiểm tra trạng thái đăng nhập hệ thống để cá nhân hóa dữ liệu công việc
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            views.setTextViewText(R.id.widgetWord, "Smart Flashcard");
            views.setTextViewText(R.id.widgetMeaning, "Vui lòng đăng nhập để xem từ vựng!");
            finalizeWidgetConfiguration(context, appWidgetManager, appWidgetId, views);
            return; // Dừng toàn bộ tiến trình phía dưới nếu chưa có user hợp lệ
        }

        String currentUid = currentUser.getUid();

        // 4. Kết nối Cloud Firestore: Thực hiện truy vấn bóc tách dữ liệu theo cấp bậc chuẩn xác
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Đi từ gốc danh mục bộ bài (decks), tiến hành lọc theo ownerId của user hiện tại
        db.collection("decks")
                .whereEqualTo("ownerId", currentUid)
                .get()
                .addOnCompleteListener(deckTask -> {
                    if (deckTask.isSuccessful() && deckTask.getResult() != null && !deckTask.getResult().isEmpty()) {

                        List<QueryDocumentSnapshot> deckList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : deckTask.getResult()) {
                            deckList.add(doc);
                        }

                        // Chọn ngẫu nhiên một bộ bài bất kỳ sở hữu bởi User này
                        Random random = new Random();
                        QueryDocumentSnapshot randomDeck = deckList.get(random.nextInt(deckList.size()));

                        // Đi sâu thẳng vào subcollection "flashcards" nằm bên trong bộ bài đó
                        randomDeck.getReference().collection("flashcards")
                                .get()
                                .addOnCompleteListener(flashcardTask -> {
                                    if (flashcardTask.isSuccessful() && flashcardTask.getResult() != null && !flashcardTask.getResult().isEmpty()) {

                                        List<QueryDocumentSnapshot> cardList = new ArrayList<>();
                                        for (QueryDocumentSnapshot cardDoc : flashcardTask.getResult()) {
                                            cardList.add(cardDoc);
                                        }

                                        // Chọn ngẫu nhiên một tấm thẻ từ vựng thực tế để hiển thị lên Widget
                                        QueryDocumentSnapshot randomCard = cardList.get(random.nextInt(cardList.size()));

                                        String word = randomCard.getString("front");
                                        String meaning = randomCard.getString("back");

                                        // Đẩy dữ liệu chữ thực tế lên các TextView đích trên Widget
                                        views.setTextViewText(R.id.widgetWord, word);
                                        views.setTextViewText(R.id.widgetMeaning, meaning);

                                    } else {
                                        if (!flashcardTask.isSuccessful()) {
                                            String errorMsg = flashcardTask.getException() != null ? flashcardTask.getException().getMessage() : "Unknown Error";
                                            views.setTextViewText(R.id.widgetWord, "Cards Error");
                                            views.setTextViewText(R.id.widgetMeaning, errorMsg);
                                        } else {
                                            views.setTextViewText(R.id.widgetWord, "Empty Deck");
                                            views.setTextViewText(R.id.widgetMeaning, "Bộ bài được chọn hiện không có từ vựng nào!");
                                        }
                                    }

                                    // Đóng gói cấu hình và đẩy luồng hiển thị hoàn chỉnh lên UI
                                    finalizeWidgetConfiguration(context, appWidgetManager, appWidgetId, views);
                                });
                    } else {
                        if (!deckTask.isSuccessful()) {
                            String errorMsg = deckTask.getException() != null ? deckTask.getException().getMessage() : "Unknown Error";
                            views.setTextViewText(R.id.widgetWord, "Decks Error");
                            views.setTextViewText(R.id.widgetMeaning, errorMsg);
                        } else {
                            views.setTextViewText(R.id.widgetWord, "No Decks");
                            views.setTextViewText(R.id.widgetMeaning, "Bạn chưa tạo bộ bài nào. Hãy vào ứng dụng để thêm mới!");
                        }
                        finalizeWidgetConfiguration(context, appWidgetManager, appWidgetId, views);
                    }
                });
    }

    // Hàm phụ đóng gói Intent chuyển hướng mở ứng dụng chính khi chạm vào vùng chữ của Widget
    private static void finalizeWidgetConfiguration(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews views) {
        Intent intent = new Intent(context, ChoiceLoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widgetWord, pendingIntent);

        // Ra lệnh trực tiếp cho Widget Manager cập nhật Layout cuối cùng lên Home Screen
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}