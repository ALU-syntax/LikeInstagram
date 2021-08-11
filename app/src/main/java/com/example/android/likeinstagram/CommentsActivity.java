package com.example.android.likeinstagram;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.likeinstagram.Adapter.CommentsAdapter;
import com.example.android.likeinstagram.Model.Comments;
import com.example.android.likeinstagram.Model.Users;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private EditText commentEdit;
    private Button mAddCommentBtn;
    private RecyclerView mCommentRecyclerView;
    private FirebaseFirestore firestore;
    private String post_id;
    private String currentUserId;
    private FirebaseAuth auth;
    private CommentsAdapter adapter;
    private List<Comments> mList;
    private List<Users> usersList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        commentEdit = findViewById(R.id.comment_edittext);
        mAddCommentBtn = findViewById(R.id.add_comment);
        mCommentRecyclerView = findViewById(R.id.comment_recyclerView);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        mList = new ArrayList<>();
        usersList = new ArrayList<>();
        adapter = new CommentsAdapter(CommentsActivity.this, mList, usersList);

        post_id = getIntent().getStringExtra("postid");
        mCommentRecyclerView.setHasFixedSize(true);
        mCommentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mCommentRecyclerView.setAdapter(adapter);

        firestore.collection("Posts/" + post_id + "/Comments").addSnapshotListener(CommentsActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable @org.jetbrains.annotations.Nullable QuerySnapshot value, @Nullable @org.jetbrains.annotations.Nullable FirebaseFirestoreException error) {
               for(DocumentChange documentChange: value.getDocumentChanges()){
                   if (documentChange.getType() ==DocumentChange.Type.ADDED){
                       Comments comments = documentChange.getDocument().toObject(Comments.class);
                       String userId = documentChange.getDocument().getString("user");
                       firestore.collection("Users").document(userId).get()
                               .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                   @Override
                                   public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                                       if (task.isSuccessful()){
                                            Users users = task.getResult().toObject(Users.class);
                                            usersList.add(users);
                                            mList.add(comments);
                                            adapter.notifyDataSetChanged();
                                       }else{
                                           Toast.makeText(CommentsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                       }
                                   }
                               });
                   }else {
                       adapter.notifyDataSetChanged();
                   }
               }
            }
        });

        mAddCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String comment = commentEdit.getText().toString();
                if (!comment.isEmpty()){
                    Map<String, Object> commentsMap = new HashMap<>();
                    commentsMap.put("comment", comment);
                    commentsMap.put("time" , FieldValue.serverTimestamp());
                    commentsMap.put("user", currentUserId);
                    firestore.collection("Posts" + post_id + "/Comments").add(commentsMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<DocumentReference> task) {
                            if (task.isSuccessful()){
                                Toast.makeText(CommentsActivity.this, "Comment Added!", Toast.LENGTH_SHORT).show();
                            }else{
                                Toast.makeText(CommentsActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    Toast.makeText(CommentsActivity.this, "Please Write Comment!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}