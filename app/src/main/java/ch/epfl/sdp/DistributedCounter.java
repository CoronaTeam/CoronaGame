package ch.epfl.sdp;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DistributedCounter {

    public class Counter {
        int numShards;

        public Counter(int numShards) {
            this.numShards = numShards;
        }
    }

    public class Shard {
        int count;

        public Shard(int count) {
            this.count = count;
        }
    }

    public Task<Void> createCounter(final DocumentReference ref, final int numShards) {
        // Initialize the counter document, then initialize each shard.
        return ref.set(new Counter(numShards))
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    List<Task<Void>> tasks = new ArrayList<>();

                    // Initialize each shard with count=0
                    for (int i = 0; i < numShards; i++) {
                        Task<Void> makeShard = ref.collection("shards")
                                .document(String.valueOf(i))
                                .set(new Shard(0));
                        tasks.add(makeShard);
                    }
                    return Tasks.whenAll(tasks);
                });
    }

    public Task<Void> incrementCounter(final DocumentReference ref, final int numShards,
                                       String path) {
        return indecrementCounter(true, ref, numShards, path);
    }

    public Task<Void> decrementCounter(final DocumentReference ref, final int numShards,
                                       String path) {
        return indecrementCounter(false, ref, numShards, path);
    }

    public Task<Integer> getCount(final DocumentReference ref) {
        // Sum the count of each shard in the subcollection
        return ref.collection("shards").get()
                .continueWith(task -> {
                    int count = 0;
                    for (DocumentSnapshot snap :task.getResult()) {
                        Shard shard = snap.toObject(Shard.class);
                        assert shard != null;
                        count += shard.count;
                    }
                    return count;
                });
    }

    private Task<Void> indecrementCounter(boolean isIncrement, final DocumentReference ref,
                                          final int numShards, String path){
        int shardId = (int) Math.floor(Math.random() * numShards);
        DocumentReference shardRef = ref.collection(path).document(String.valueOf(shardId));
        return shardRef.update("count", FieldValue.increment(isIncrement ? 1 : -1));
    }
}


