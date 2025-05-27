package com.example.eduease;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Home extends BaseActivity implements QuizAdapter.QuizClickListener {

    private EditText searchQuiz;
    private QuizAdapter quizAdapter;
    private List<Quiz> quizList;  // Original quiz list from Firestore
    private List<Quiz> filteredQuizList;  // Filtered quiz list for search
    private FirebaseFirestore db;
    private MediaPlayer mediaPlayer;

    private Spinner my_spinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);





        ///DROP DOWN
        Spinner spinner = findViewById(R.id.my_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.dropdown_items, R.layout.spinner_item); // For selected item (no line)
        adapter.setDropDownViewResource(R.layout.spinner_item_divider); // For dropdown list items
        spinner.setAdapter(adapter);



        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean isFirstSelection = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Skip the first automatic selection
                if (isFirstSelection) {
                    isFirstSelection = false;
                    return;
                }

                String selectedItem = parent.getItemAtPosition(position).toString().trim();

                switch (selectedItem) {
                    case "Local Quiz":
                        loadQuizzesFromRealtimeDB();
                        break;
                    case "Public quiz":
                        vibrate();
                        loadPublicQuizzesFromRealtime();
                        break;
                    case "Local Bonus Flash":
                        loadLocalBonusFlashQuizzesFromRealtime();
                        break;
                    case "Bonus Flash":
                        vibrate();
                        loadBonusFlashQuizzesFromRealtime();
                        break;
                    case "Random Quiz":
                        loadRandomQuizTopicsFromRealtime();
                        break;
                    default:
                        // Optional: handle unknown selection
                        break;
                }
            }

     

        @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Optional: handle no selection
            }
        });




        boolean skipMusic = getIntent().getBooleanExtra("SKIP_MUSIC", false);
        if (!skipMusic) {
            playOpeningSound(); // Play the opening sound only if skipMusic is false
        }

        applyEdgeToEdgePadding();
        loadProfileImage();
        setupProfileClickListener();
        setupCreateButtonClickListener();

        RecyclerView quizzesRecyclerView = findViewById(R.id.quizzes_recycler_view);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2); // 2 columns
        quizzesRecyclerView.setLayoutManager(gridLayoutManager);

        searchQuiz = findViewById(R.id.search_quiz);
        quizList = new ArrayList<>();
        filteredQuizList = new ArrayList<>();
        quizAdapter = new QuizAdapter(filteredQuizList, this);
        quizzesRecyclerView.setAdapter(quizAdapter);

        db = FirebaseFirestore.getInstance();
        loadQuizzesFromRealtimeDB();

        setupSearchListener();



    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadBonusFlashQuizzesFromRealtime() {
        showLoading(); // Show loading indicator at start

        AppCompatButton helpButton = findViewById(R.id.help_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helpButton.setTooltipText("A public topic-based quiz with random bonus points that created by you and others. Correct answers earn the bonus based on the box you choose.");
        }




        FirebaseApp secondaryApp;

        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary");
        }

        quizList.clear();
        filteredQuizList.clear();

        FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
        DatabaseReference bonusFlashRef = secondaryDatabase.getReference("bonus_quizzes");

        bonusFlashRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                quizList.clear();

                for (DataSnapshot quizSnapshot : dataSnapshot.getChildren()) {
                    String quizTitle = "";
                    String description = "";
                    boolean isFlash = true;
                    String type = "";

                    if (quizSnapshot.child("title").exists()) {
                        quizTitle = quizSnapshot.child("title").getValue(String.class);
                    }

                    if (quizSnapshot.child("description").exists()) {
                        description = quizSnapshot.child("description").getValue(String.class);
                    }

                    if (quizSnapshot.child("flash").exists()) {
                        isFlash = quizSnapshot.child("flash").getValue(Boolean.class);
                    }

                    if (quizSnapshot.child("type").exists()) {
                        type = quizSnapshot.child("type").getValue(String.class);
                    }

                    if ("public".equalsIgnoreCase(type)) {
                        Quiz quiz = new Quiz();
                        quiz.setTitle(quizTitle);
                        quiz.setDescription(description);
                        quiz.setId(quizSnapshot.getKey());
                        quiz.setFlash(isFlash);

                        quizList.add(quiz);
                    }
                }

                filteredQuizList.clear();
                filteredQuizList.addAll(quizList);
                quizAdapter.notifyDataSetChanged();

                RecyclerView recyclerView = findViewById(R.id.quizzes_recycler_view);
                TextView emptyView = findViewById(R.id.empty_view);
                TextView quizTitle = findViewById(R.id.quiz_title);

                if (quizList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                quizTitle.setText("Public Bonus Flash");

                hideLoading(); // Hide loading indicator once data is loaded
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Home", "Error fetching bonus flash quizzes", databaseError.toException());
                hideLoading(); // Hide loading even on error
            }
        });
    }





    @SuppressLint("NotifyDataSetChanged")
    private void loadLocalBonusFlashQuizzesFromRealtime() {
        showLoading();

        AppCompatButton helpButton = findViewById(R.id.help_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helpButton.setTooltipText("A private and local topic-based quiz with random bonus points. Correct answers earn the bonus based on the box you choose.");
        }


        FirebaseApp secondaryApp;

        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();
            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary");
        }

        quizList.clear();
        filteredQuizList.clear();

        FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
        DatabaseReference bonusFlashRef = secondaryDatabase.getReference("bonus_quizzes");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getUid();

        bonusFlashRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                quizList.clear();  // Clear existing quiz list to avoid duplicates

                // Loop through each bonus flash quiz (root level)
                for (DataSnapshot quizSnapshot : dataSnapshot.getChildren()) {
                    // Get the title, description, flash, and type field directly from the data inside the snapshot
                    String quizTitle = "";
                    String description = "";
                    boolean isFlash = true; // Default to false, will change if found
                    String type = ""; // New variable to store the type

                    // Retrieve the title and description
                    if (quizSnapshot.child("title").exists()) {
                        quizTitle = quizSnapshot.child("title").getValue(String.class);
                    }

                    if (quizSnapshot.child("description").exists()) {
                        description = quizSnapshot.child("description").getValue(String.class);
                    }

                    // Check if the "flash" field exists and set it accordingly
                    if (quizSnapshot.child("flash").exists()) {
                        isFlash = quizSnapshot.child("flash").getValue(Boolean.class);
                    }

                    // Check if the "type" field exists and set it accordingly
                    if (quizSnapshot.child("type").exists()) {
                        type = quizSnapshot.child("type").getValue(String.class);
                    }

                    // Only add quizzes that are of type "public"
                    if ("local".equalsIgnoreCase(type)) {
                        // Create a Quiz object and add it to the list
                        Quiz quiz = new Quiz();
                        quiz.setTitle(quizTitle);  // Set the title from the data
                        quiz.setDescription(description);  // Set the description
                        quiz.setId(quizSnapshot.getKey()); // Using the snapshot key as ID (root)
                        quiz.setFlash(isFlash); // Mark as Bonus Flash quiz if flash is true

                        quizList.add(quiz);  // Add quiz to the list
                    }
                }

                filteredQuizList.clear();
                filteredQuizList.addAll(quizList);
                quizAdapter.notifyDataSetChanged();

                // Handle empty view visibility and title update
                RecyclerView recyclerView = findViewById(R.id.quizzes_recycler_view);
                TextView emptyView = findViewById(R.id.empty_view);
                TextView quizTitle = findViewById(R.id.quiz_title);  // Reference to quiz title TextView

                if (quizList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);  // Show the empty view
                    recyclerView.setVisibility(View.GONE);  // Hide the RecyclerView
                    quizTitle.setText("Public Bonus Flash");  // Set the title to "Public Bonus Flash"
                } else {
                    emptyView.setVisibility(View.GONE);     // Hide the empty view
                    recyclerView.setVisibility(View.VISIBLE);  // Show the RecyclerView
                    quizTitle.setText("Public Bonus Flash");  // Restore the original title
                }

                hideLoading();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Home", "Error fetching local bonus flash quizzes", databaseError.toException());
                hideLoading();
            }
        });
    }


    @SuppressLint("NotifyDataSetChanged")
    private void loadPublicQuizzesFromRealtime() {
        showLoading();

        AppCompatButton helpButton = findViewById(R.id.help_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helpButton.setTooltipText("Quizzes that shared publicly by you and others — anyone online can view and take them.");
        }

        FirebaseApp secondaryApp;

        // Initialize secondary FirebaseApp only if not already initialized
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary");
        }

        quizList.clear();
        filteredQuizList.clear();

        FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
        DatabaseReference publicQuizzesRef = secondaryDatabase.getReference("public_quizzes");

        publicQuizzesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                quizList.clear();  // Clear existing quiz list to avoid duplicates

                for (DataSnapshot quizSnapshot : dataSnapshot.getChildren()) {
                    String quizTitle = "";
                    String description = "";

                    if (quizSnapshot.child("title").exists()) {
                        quizTitle = quizSnapshot.child("title").getValue(String.class);
                    }

                    if (quizSnapshot.child("description").exists()) {
                        description = quizSnapshot.child("description").getValue(String.class);
                    }

                    Quiz quiz = new Quiz();
                    quiz.setTitle(quizTitle);
                    quiz.setDescription(description);
                    quiz.setId(quizSnapshot.getKey());
                    quiz.setTypeQuiz("public");

                    quizList.add(quiz);
                }

                filteredQuizList.clear();
                filteredQuizList.addAll(quizList);
                quizAdapter.notifyDataSetChanged();

                // Handle empty view visibility and title update
                RecyclerView recyclerView = findViewById(R.id.quizzes_recycler_view);
                TextView emptyView = findViewById(R.id.empty_view);
                TextView quizTitleText = findViewById(R.id.quiz_title);  // Reference to quiz title TextView

                if (quizList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    quizTitleText.setText("Public Quizzes");  // Update title when no quizzes
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    quizTitleText.setText("Public Quizzes");  // Always show Public Quizzes title when loading public quizzes
                }

                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Home", "Error fetching public quizzes", databaseError.toException());
                hideLoading();
            }
        });
    }



    @SuppressLint("NotifyDataSetChanged")
    private void loadRandomQuizTopicsFromRealtime() {
        showLoading();

        AppCompatButton helpButton = findViewById(R.id.help_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helpButton.setTooltipText("A mix of random quiz topics from different categories made by admins. Choose from multiple choice, identification, or true or false modes.");
        }

        FirebaseApp secondaryApp;

        // Initialize secondary FirebaseApp if not already
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();

            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary");
        }

        quizList.clear();
        filteredQuizList.clear();

        FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
        DatabaseReference randomQuizRef = secondaryDatabase.getReference("random_quiz_topic");

        randomQuizRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                quizList.clear();

                for (DataSnapshot quizSnapshot : dataSnapshot.getChildren()) {
                    String quizTitle = "";
                    String type = "";

                    if (quizSnapshot.child("title").exists()) {
                        quizTitle = quizSnapshot.child("title").getValue(String.class);
                    }

                    if (quizSnapshot.child("type").exists()) {
                        type = quizSnapshot.child("type").getValue(String.class);
                    }

                    if (quizTitle != null && type.equals("randomquiz")) {
                        Quiz quiz = new Quiz();
                        quiz.setTitle(quizTitle);
                        quiz.setDescription(""); // Optional
                        quiz.setId(quizSnapshot.getKey());
                        quiz.setType(type); // Add this to your Quiz model

                        quizList.add(quiz);
                    }
                }


                filteredQuizList.clear();
                filteredQuizList.addAll(quizList);
                quizAdapter.notifyDataSetChanged();

                RecyclerView recyclerView = findViewById(R.id.quizzes_recycler_view);
                TextView emptyView = findViewById(R.id.empty_view);
                TextView quizTitleText = findViewById(R.id.quiz_title);

                if (quizList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                    quizTitleText.setText("Random Quiz Topics");
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    quizTitleText.setText("Random Quiz Topics");
                }

                hideLoading();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Home", "Error fetching random quiz topics", databaseError.toException());
                hideLoading();
            }
        });
    }




//    private void showQuizTypeDialog(String topicTitle) {
//        String[] quizTypes = {"Multiple Choice", "True or False", "Identification"};
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Select Quiz Type for " + topicTitle)
//                .setItems(quizTypes, (dialog, which) -> {
//                    String selectedType = quizTypes[which];
//
//                    switch (selectedType) {
//                        case "Multiple Choice":
//                            startRandomQuizActivity(topicTitle, "multiple");
//                            break;
//                        case "True or False":
//                            startRandomQuizActivity(topicTitle, "truefalse");
//                            break;
//                        case "Identification":
//                            startRandomQuizActivity(topicTitle, "identification");
//                            break;
//                    }
//                });
//        builder.show();
//    }


    private void startRandomQuizActivity(String topicTitle, String quizType) {
        Intent intent = new Intent(this, RandomQuizMultipleChoice.class);
        intent.putExtra("topicTitle", topicTitle);
        intent.putExtra("quizType", quizType);
        startActivity(intent);
    }




    private void playOpeningSound() {
        mediaPlayer = MediaPlayer.create(this, R.raw.opening_music); // Replace with your sound file in res/raw
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release MediaPlayer resources
            mediaPlayer = null;
        }
    }


    private void applyEdgeToEdgePadding() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadProfileImage() {
        String profileImageUrl = getIntent().getStringExtra("PROFILE_IMAGE_URL");
        ImageView profileImageView = findViewById(R.id.profile_image_view);

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this).load(profileImageUrl).circleCrop().into(profileImageView);
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl().toString()).circleCrop().into(profileImageView);
            }
        }
    }

    private void setupProfileClickListener() {
        ImageView profileImageView = findViewById(R.id.profile_image_view);
        profileImageView.setOnClickListener(v -> {
            vibrate(); // Trigger vibration
            startActivity(new Intent(Home.this, Settings.class));
        });
    }

    private void setupCreateButtonClickListener() {
        ImageButton createButton = findViewById(R.id.create_btn);
        createButton.setOnClickListener(v -> {
            vibrate(); // Trigger vibration
            showCreateOrImportDialog();
        });
    }

    private void showCreateOrImportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose an option")
                .setItems(new CharSequence[]{
                        "Create Quiz",
                        "Create Bonus Flash",
                        "Import Quiz"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Create Quiz
                            showLocalOrPublicQuizDialog();
                            break;
                        case 1: // Create Bonus Flash
                            showLocalOrPublicBonusFlashDialog();
                            break;
                        case 2: // Import Quiz
                            // Import Quiz logic here
                            break;
                    }
                })
                .show();
    }

    private void showLocalOrPublicBonusFlashDialog() {
        AlertDialog.Builder bonusFlashBuilder = new AlertDialog.Builder(this);
        bonusFlashBuilder.setTitle("Choose Bonus Flash Type")
                .setItems(new CharSequence[]{
                        "Local Bonus Flash",
                        "Public Bonus Flash"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Local Bonus Flash
                            startActivity(new Intent(Home.this, CreateLocalBonusFlash.class));
                            break;
                        case 1: // Public Bonus Flash
                            startActivity(new Intent(Home.this, CreateBonusFlash.class));
                            break;
                    }
                })
                .show();
    }









    private void showLocalOrPublicQuizDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Quiz Type")
                .setItems(new CharSequence[]{
                        "Local Quiz",
                        "Public Quiz"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Local Quiz
                            Intent localIntent = new Intent(Home.this, CreateQuiz.class);
                            localIntent.putExtra("quiz_type", "local");
                            startActivity(localIntent);
                            break;
                        case 1: // Public Quiz
                            Intent publicIntent = new Intent(Home.this, ayan_create_public_quizzes.class);
                            publicIntent.putExtra("quiz_type", "public");
                            startActivity(publicIntent);
                            break;
                    }
                })
                .show();
    }



    private void setupSearchListener() {
        searchQuiz.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterQuizzes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add an OnClickListener to vibrate on click
        searchQuiz.setOnClickListener(v -> vibrate());
    }


    private void filterQuizzes(String query) {
        if (query.isEmpty()) {
            filteredQuizList.clear();
            filteredQuizList.addAll(quizList);  // Show all quizzes if query is empty
        } else {
            filteredQuizList.clear();
            String lowerCaseQuery = query.toLowerCase();
            filteredQuizList.addAll(
                    quizList.stream()
                            .filter(quiz -> quiz.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                                    quiz.getDescription().toLowerCase().contains(lowerCaseQuery))
                            .collect(Collectors.toList())
            );
        }
        quizAdapter.notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadQuizzesFromRealtimeDB() {
        showLoading();

        AppCompatButton helpButton = findViewById(R.id.help_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            helpButton.setTooltipText("Your personal quizzes — only you can view and manage them.");
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.w("Home", "User not logged in.");
            return;
        }

        String userId = currentUser.getUid();

        FirebaseApp secondaryApp;
        try {
            secondaryApp = FirebaseApp.getInstance("Secondary");
        } catch (IllegalStateException e) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:882141634417:android:ac69b51d83d01def3460d0")
                    .setApiKey("AIzaSyBlECTZf28SbEc4xHsz7JnH99YtTw6T58I")
                    .setProjectId("edu-ease-ni-ayan")
                    .setDatabaseUrl("https://edu-ease-ni-ayan-default-rtdb.firebaseio.com/")
                    .build();
            secondaryApp = FirebaseApp.initializeApp(getApplicationContext(), options, "Secondary");
        }

        FirebaseDatabase secondaryDatabase = FirebaseDatabase.getInstance(secondaryApp);
        DatabaseReference quizzesRef = secondaryDatabase.getReference("local_quizzes");

        quizzesRef.orderByChild("creatorId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        quizList.clear();
                        filteredQuizList.clear();

                        for (DataSnapshot quizSnapshot : snapshot.getChildren()) {
                            Map<String, Object> quizData = (Map<String, Object>) quizSnapshot.getValue();

                            if (quizData == null) continue;

                            Quiz quiz = new Quiz();
                            quiz.setId(quizSnapshot.getKey());
                            quiz.setCreatorId((String) quizData.get("creatorId"));
                            quiz.setTitle((String) quizData.get("title"));
                            quiz.setDescription((String) quizData.get("description"));
                            quiz.setType((String) quizData.get("type")); // Optional if you track type

                            quizList.add(quiz);
                        }

                        filteredQuizList.addAll(quizList);
                        quizAdapter.notifyDataSetChanged();

                        // UI updates
                        RecyclerView recyclerView = findViewById(R.id.quizzes_recycler_view);
                        TextView emptyView = findViewById(R.id.empty_view);
                        TextView quizTitleText = findViewById(R.id.quiz_title);

                        if (quizList.isEmpty()) {
                            emptyView.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            quizTitleText.setText("Local Quizzes");
                            hideLoading();
                        } else {
                            emptyView.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            quizTitleText.setText("Local Quizzes");
                            hideLoading();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        hideLoading();
                        Log.e("Home", "Failed to load local quizzes", error.toException());
                    }
                });
    }



    @Override
    public void onQuizClick(Quiz quiz) {
        vibrate(); // Optional

        // Log the value of isFlash
        Log.d("Home", "Quiz isFlash: " + quiz.isFlash());

        // Bypass options dialog if it's a Bonus Flash quiz
        if (quiz.isFlash()) {
            Toast.makeText(Home.this, "Bonus Flash", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Home.this, TakeBonusFlash.class);
            intent.putExtra("quizId", quiz.getId());
            startActivity(intent);
            return;
        }

        // Show options dialog for regular quizzes only
        showQuizOptionsDialog(quiz);
    }




    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            }
        }
    }

    private void showQuizOptionsDialog(Quiz quiz) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Choose an option")
                .setItems(new CharSequence[]{
                        "Edit",
                        "Delete",
                        "Review",
                        "Quiz"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit Quiz
                            Intent editIntent = new Intent(Home.this, CreateQuiz.class);
                            editIntent.putExtra("QUIZ_ID", quiz.getId());
                            startActivity(editIntent);
                            break;
                        case 1: // Delete Quiz
                            deleteQuiz(quiz);
                            break;
                        case 2: // Review (Flashcards)
                            break;
                        case 3: // Take Quiz
                            showQuizTypeDialog(quiz);
                            break;
                    }
                })
                .show();
    }

    private void showQuizTypeDialog(Quiz quiz) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Quiz Type")
                .setItems(new CharSequence[]{
                        "Identification",
                        "Multiple Choice",
                        "True or False"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Identification
                            Intent takeIdentificationIntent = new Intent(Home.this, TakeQuiz.class);
                            takeIdentificationIntent.putExtra("QUIZ_ID", quiz.getId());
                            takeIdentificationIntent.putExtra("QUIZ_TYPE", "Identification");
                            startActivity(takeIdentificationIntent);
                            break;
                        case 1: // Multiple Choice
                            break;
                        case 2: // True or False
                            break;
                    }
                })
                .show();
    }


    private void deleteQuiz(Quiz quiz) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(this);
        confirmDialog.setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete this quiz?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("quizzes").document(quiz.getId())
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove the quiz from both lists
                                quizList.remove(quiz);
                                filteredQuizList.remove(quiz);
                                quizAdapter.notifyDataSetChanged();
                                Log.d("Home", "Quiz successfully deleted!");
                            })
                            .addOnFailureListener(e -> Log.e("Home", "Error deleting quiz", e));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

