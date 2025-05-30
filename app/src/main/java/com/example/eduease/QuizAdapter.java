package com.example.eduease;

import android.app.AlertDialog;
import android.content.Intent;
import android.util.Log;
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
                String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

                List<CharSequence> options = new java.util.ArrayList<>();
                List<Runnable> actions = new java.util.ArrayList<>();

                boolean isCreator = currentUserId.equals(currentQuiz.getCreatorId());

                if (isCreator) {
                    options.add("Edit");
                    actions.add(() -> {
                        Intent editIntent = new Intent(v.getContext(), Edit_Create_Local_Bonus_Flash.class);
                        editIntent.putExtra("QUIZ_ID", currentQuiz.getId());
                        editIntent.putExtra("QUIZ_TITLE", currentQuiz.getTitle()); // This might be redundant if title is loaded from DB in Edit_Create_Local_Bonus_Flash
                        v.getContext().startActivity(editIntent);
                    });

                    options.add("Delete");
                    actions.add(() -> {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this bonus flash?")
                                .setPositiveButton("Delete", (dialogInterface, i) -> {
                                    DatabaseReference quizRef = FirebaseDatabase.getInstance()
                                            .getReference("bonus_quizzes")
                                            .child(currentQuiz.getId());

                                    // Get the type of the quiz BEFORE deletion, as currentQuiz might become invalid
                                    // if it's part of a list that gets cleared by the refresh.
                                    String quizType = currentQuiz.getType();

                                    quizRef.removeValue()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(v.getContext(), "Quiz deleted successfully!", Toast.LENGTH_SHORT).show();

                                                // Ensure the context is the Home activity before attempting to refresh
                                                if (v.getContext() instanceof Home) {
                                                    Home homeActivity = (Home) v.getContext();

                                                    // Conditionally call the appropriate refresh method based on quizType
                                                    if ("local".equalsIgnoreCase(quizType)) {
                                                        homeActivity.loadLocalBonusFlashQuizzesFromRealtime();
                                                    } else {
                                                        // Assuming loadBonusFlashQuizzesFromRealtime() handles non-local bonus flashes
                                                        // You might need to confirm the exact name of this method in your Home activity
                                                        homeActivity.loadBonusFlashQuizzesFromRealtime();
                                                    }
                                                } else {
                                                    Log.w("QuizDelete", "Context is not Home activity, cannot refresh directly.");
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(v.getContext(), "Failed to delete quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }

                options.add("Take Bonus Flash");
                actions.add(() -> {
                    Intent intent = new Intent(v.getContext(), TakeBonusFlash.class);
                    intent.putExtra("quizId", currentQuiz.getId());
                    intent.putExtra("QUIZ_TITLE", currentQuiz.getTitle());
//                    intent.putExtra("type", "public");
                    v.getContext().startActivity(intent);
                });

                AlertDialog.Builder builder;
                try {
                    builder = new AlertDialog.Builder(v.getContext(), R.style.CustomAlertDialog);
                } catch (Exception e) {
                    builder = new AlertDialog.Builder(v.getContext()); // fallback in case of error
                }

                builder.setTitle("Choose an option")
                        .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> actions.get(which).run())
                        .show();

            } else if ("randomquiz".equals(currentQuiz.getType())) {


                FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance();
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
                String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

                List<CharSequence> options = new java.util.ArrayList<>();
                List<Runnable> actions = new java.util.ArrayList<>();

                boolean isCreator = currentUserId.equals(currentQuiz.getCreatorId());

                // This code snippet would be wherever your QuizAdapter or similar setup is
// It assumes `currentQuiz` has a `getType()` method.
                if (isCreator) {
                    options.add("Edit");
                    actions.add(() -> {
                        Intent editIntent = new Intent(v.getContext(), Edit_Create_Local_Bonus_Flash.class);
                        editIntent.putExtra("QUIZ_ID", currentQuiz.getId());
                        editIntent.putExtra("QUIZ_TITLE", currentQuiz.getTitle());
                        v.getContext().startActivity(editIntent);
                    });

                    options.add("Delete");
                    actions.add(() -> {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this quiz?")
                                .setPositiveButton("Delete", (dialogInterface, i) -> {
                                    // IMPORTANT: Ensure you are using the correct FirebaseApp instance if you have multiple.
                                    // If "bonus_quizzes" is part of a named FirebaseApp (e.g., "secondary" or "bonusFlashApp"),
                                    // you must use: FirebaseDatabase.getInstance(FirebaseApp.getInstance("secondary")).getReference("bonus_quizzes")
                                    // If it's part of the default app, then FirebaseDatabase.getInstance() is fine.
                                    DatabaseReference quizRef = FirebaseDatabase.getInstance()
                                            .getReference("bonus_quizzes") // Corrected path for bonus quizzes
                                            .child(currentQuiz.getId());

                                    quizRef.removeValue()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(v.getContext(), "Quiz deleted successfully!", Toast.LENGTH_SHORT).show();
                                                // *** NO EXPLICIT REFRESH CALL HERE ***
                                                // The `addValueEventListener` in `loadLocalBonusFlashQuizzesFromRealtime`
                                                // will automatically detect this deletion and update the UI.
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(v.getContext(), "Failed to delete quiz: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });
                }

                options.add("Take Quiz");
                actions.add(() -> {
                    Intent intent = new Intent(v.getContext(), TakePublicQuiz.class);
                    intent.putExtra("quizId", currentQuiz.getId());
                    intent.putExtra("typeQuiz", "public");
                    v.getContext().startActivity(intent);
                });

                AlertDialog.Builder builder;
                try {
                    builder = new AlertDialog.Builder(v.getContext(), R.style.CustomAlertDialog);
                } catch (Exception e) {
                    builder = new AlertDialog.Builder(v.getContext()); // fallback in case of error
                }

                builder.setTitle("Choose an option")
                        .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> actions.get(which).run())
                        .show();


        } else {
                String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

                List<CharSequence> options = new java.util.ArrayList<>();
                List<Runnable> actions = new java.util.ArrayList<>();

                boolean isCreator = currentUserId.equals(currentQuiz.getCreatorId());

                if (isCreator) {
                    options.add("Edit");
                    actions.add(() -> {
                        Intent editIntent = new Intent(v.getContext(), Edit_Create_Local_Quiz.class);
                        editIntent.putExtra("QUIZ_ID", currentQuiz.getId());
                        v.getContext().startActivity(editIntent);
                    });


                    options.add("Delete");
                    actions.add(() -> {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Confirm Delete")
                                .setMessage("Are you sure you want to delete this quiz?")
                                .setPositiveButton("Delete", (dialogInterface, i) -> {
                                    DatabaseReference quizRef = FirebaseDatabase.getInstance()
                                            .getReference("local_quizzes")
                                            .child(currentQuiz.getId());

                                    quizRef.removeValue()
                                            .addOnSuccessListener(aVoid ->
                                                    Toast.makeText(v.getContext(), "Quiz deleted", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(v.getContext(), "Delete failed", Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    });

                }

                options.add("Take Quiz");
                actions.add(() -> {
                    Intent intent = new Intent(v.getContext(), TakeQuiz.class);
                    intent.putExtra("QUIZ_ID", currentQuiz.getId());
                    intent.putExtra("typeQuiz", "local");
                    v.getContext().startActivity(intent);
                });

                AlertDialog.Builder builder;
                try {
                    builder = new AlertDialog.Builder(v.getContext(), R.style.CustomAlertDialog);
                } catch (Exception e) {
                    builder = new AlertDialog.Builder(v.getContext()); // fallback in case of error
                }

                builder.setTitle("Choose an option")
                        .setItems(options.toArray(new CharSequence[0]), (dialog, which) -> actions.get(which).run())
                        .show();


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
