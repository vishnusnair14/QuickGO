package com.vishnu.quickgovendor.ui.product;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vishnu.quickgovendor.databinding.FragmentEditProductBinding;
import com.vishnu.quickgovendor.vmodels.EditProductVM;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditProductFragment extends Fragment {
    private DocumentReference shopIdRef;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String shopID;
    private ProgressBar lpb;
    private TextView ltv;
    EditProductAdapter adapterEditProduct;

    public EditProductFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        shopID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
        shopIdRef = db.collection("ShopData").document("itemData").collection("allAvailableItems").document(shopID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        com.vishnu.quickgovendor.databinding.FragmentEditProductBinding binding = com.vishnu.quickgovendor.databinding.FragmentEditProductBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ltv = binding.editProductLoadingTextView;
        lpb = binding.editProductLoadingProgressBar;

        syncEditProductRecycleView(binding);
        return root;
    }

    @SuppressLint("NotifyDataSetChanged")
    @SuppressWarnings("unchecked")
    public void syncEditProductRecycleView(FragmentEditProductBinding binding) {
        RecyclerView recyclerView = binding.editProductRecycleView;
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        recyclerView.setLayoutManager(layoutManager);

        List<EditProductVM> itemList = new ArrayList<>();

        // Retrieve data from Firestore db
        shopIdRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    // Retrieve all field names from the document
                    Map<String, Object> documentData = document.getData();
                    if (documentData != null) {
                        Log.d(TAG, "DATA: " + documentData);

                        for (String field : documentData.keySet()) {
                            Map<String, Object> dataMap = (Map<String, Object>) documentData.get(field);
                            if (dataMap != null) {
                                String item_name = (String) dataMap.get("item_name");
                                String item_image_url = (String) dataMap.get("item_image_url");
                                String item_name_reference = (String) dataMap.get("item_name_reference");
                                Long item_price = (Long) dataMap.get("item_price");
                                assert item_name != null;
                                Log.d(TAG, item_name);
                                int price = 0;
                                if (item_price != null) {
                                    price = Math.toIntExact(item_price);
                                }
                                EditProductVM item = new EditProductVM(item_image_url, item_name, item_name_reference, price);
                                itemList.add(item);
                            }
                        }
                        // Update the RecyclerView adapter
                        adapterEditProduct.notifyDataSetChanged();
                    } else {
                        Log.d("EditProductFragment", "Data Doc is empty");
                    }
                }
            } else {
                Log.d("EditProductFragment", "Error loading items: ", task.getException());
            }
        });

        adapterEditProduct = new EditProductAdapter(itemList, requireContext(), ltv, lpb);
        recyclerView.setAdapter(adapterEditProduct);
    }
}