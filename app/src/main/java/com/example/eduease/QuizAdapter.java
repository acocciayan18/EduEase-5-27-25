package com.example.eduease;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class QuizAdapter extends RecyclerView.Adapter<QuizAdapter.QuizViewHolder> {

    private List<Quiz> quizList;
    private QuizClickListener listener;

    public interface QuizClickListener {
        void onQuizClick(Quiz quiz);
    }

    public QuizAdapter(List<Quiz> quizList, QuizClickListener listener) {
        this.quizList = quizList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuizViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.quiz_item, parent, false);
        return new QuizViewHolder(v);
    }

    @Override
    public void onBindViewHolder(QuizViewHolder holder, int position) {
        Quiz currentQuiz = quizList.get(position);

        holder.titleView.setText(currentQuiz.getTitle());
        holder.descriptionView.setText(currentQuiz.getDescription());

        holder.itemView.setOnClickListener(v -> {
            if (currentQuiz.isFlash()) {
                Intent intent = new Intent(v.getContext(), TakeBonusFlash.class);
                intent.putExtra("quizId", currentQuiz.getId());
                v.getContext().startActivity(intent);
            } else if ("randomquiz".equals(currentQuiz.getType())) {

                FirebaseApp secondaryApp = FirebaseApp.getInstance("Secondary");
                FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
                DatabaseReference quizRef = secondaryDatabase
                        .getReference("random_quiz_topic")
                        .child(currentQuiz.getId());

                quizRef.child("prompt").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String prompt = snapshot.getValue(String.class);

                        String[] quizTypes = {"Multiple Choice", "True or False", "Identification"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setTitle("Select Quiz Type for " + currentQuiz.getTitle());
                        builder.setItems(quizTypes, (dialog, which) -> {
                            Class<?> targetActivity;

                            switch (which) {
                                case 0: targetActivity = RandomQuizMultipleChoice.class; break;
                                case 1: targetActivity = RandomQuizTrueOrFalse.class; break;
                                case 2: targetActivity = RandomQuizIdentification.class; break;
                                default: return;
                            }

                            Intent intent = new Intent(v.getContext(), targetActivity);
                            intent.putExtra("prompt", prompt);
                            intent.putExtra("topicTitle", currentQuiz.getTitle());
                            v.getContext().startActivity(intent);
                        });
                        builder.show();
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(v.getContext(), "Failed to fetch prompt.", Toast.LENGTH_SHORT).show();
                    }
                });

            } else if ("public".equals(currentQuiz.getTypeQuiz())) {
                Intent intent = new Intent(v.getContext(), TakePublicQuiz.class);
                intent.putExtra("quizId", currentQuiz.getId());
                intent.putExtra("typeQuiz", "public");
                v.getContext().startActivity(intent);

            } else {
                if (listener != null) {
                    listener.onQuizClick(currentQuiz);
                }
            }
        });


    }


    @Override
    public int getItemCount() {
        return quizList.size();
    }

    public static class QuizViewHolder extends RecyclerView.ViewHolder {
        public TextView titleView;
        public TextView descriptionView;

        public QuizViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.title);
            descriptionView = itemView.findViewById(R.id.description);
        }
    }



    public void updateList(List<Quiz> newQuizList) {
        if (newQuizList != null) {
            quizList.clear();
            quizList.addAll(newQuizList);
            notifyDataSetChanged();
        }
    }
}
