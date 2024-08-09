package com.vishnu.quickgovendor.ui.product;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.vishnu.quickgovendor.R;
import com.vishnu.quickgovendor.callbacks.getImageUrl;
import com.vishnu.quickgovendor.callbacks.isEmailReg;
import com.vishnu.quickgovendor.databinding.FragmentAddProductBinding;
import com.vishnu.quickgovendor.misellenous.Utils;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddProductFragment extends Fragment {
    EditText addItemNameET, addItemPriceET, addItemPriceUnitET;
    private ImageView selectedImageIV;
    private TextView imageUploadFdbkTV;
    private Uri selectedImageUri;
    private FirebaseFirestore db;
    private Button addProductBtn;
    private DocumentReference retailerAddNewProductRef;
    private TextView finalPriceViewTV;
    private DocumentReference retailerIDRef;
    private StorageReference storageRef;
    private final String LOG_TAG = this.getClass().getSimpleName();
    private final Map<String, Object> dataMap1 = new HashMap<>();
    private final Map<String, Object> dataMap = new HashMap<>();

    private FragmentAddProductBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentAddProductBinding.inflate(inflater, container, false);

        storageRef = FirebaseStorage.getInstance().getReference().child("vendorProductDescImages");

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        assert user != null;
        retailerAddNewProductRef = db.collection("ShopData").document("itemData")
                .collection("allAvailableItems").document(user.getUid());

        retailerIDRef = db.collection("AuthenticationData").document("RegisteredUsersEmail");

        selectedImageIV = binding.selectedImageImageView;
        addItemNameET = binding.itemNameEditText;
        addItemPriceET = binding.itemPriceEditText;
        addItemPriceUnitET = binding.priceUnitEditText;
        addProductBtn = binding.addProductButton;
        imageUploadFdbkTV = binding.imageUploadFeedbackSigninTextView;
        finalPriceViewTV = binding.finalPriceViewAddProductTextView;

        finalPriceViewTV.setText(MessageFormat.format("{0}/{1}", addItemPriceET.getText().toString(), addItemPriceUnitET.getText().toString()));

        addItemPriceET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                finalPriceViewTV.setText(MessageFormat.format("{0}/{1}", addItemPriceET.getText().toString(), addItemPriceUnitET.getText().toString()));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        addItemPriceUnitET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                finalPriceViewTV.setText(MessageFormat.format("{0}/{1}", addItemPriceET.getText().toString(), addItemPriceUnitET.getText().toString()));

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        addProductBtn.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                isEmailRegistered(user.getEmail(), _state -> {
                    if (!_state) {
                        Toast.makeText(requireContext(), "Reference for ID:\n" + user.getEmail() + "\nnot found in our database!", Toast.LENGTH_SHORT).show();
                    } else {
                        addProductBtn.setEnabled(false);
                        addProductBtn.setText(R.string.please_wait);
                        uploadImageToStorage(selectedImageUri, imgURL -> {
                            if (!imgURL.isEmpty()) {
                                addNewProductToDB(getView(), imgURL, user.getUid());
                            } else {
                                Toast.makeText(requireContext(), "Invalid upload image url!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            } else {
                Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            }
        });

        // Set up image selection
        ActivityResultLauncher<String> pickImage = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                displaySelectedImage(uri);
            }
        });

        selectedImageIV.setOnClickListener(v -> {
            imageUploadFdbkTV.setText("");
            pickImage.launch("image/*");
        });

        View root = binding.getRoot();

        return root;
    }

    private void displaySelectedImage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                selectedImageIV.setImageBitmap(BitmapFactory.decodeStream(inputStream));
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    private void isEmailRegistered(String email, isEmailReg isReg) {
        retailerIDRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    List<String> emails = (List<String>) document.get("email_addresses");
                    if (emails != null && emails.contains(email)) {
                        Log.i(LOG_TAG, "Email is registered: " + email);
                        isReg.isRegistered(true);
                    } else {
                        Log.i(LOG_TAG, "Email is not registered: " + email);
                        isReg.isRegistered(false);
                    }
                } else {
                    Log.i(LOG_TAG, "Document does not exist.");
                    isReg.isRegistered(false);
                }
            } else {
                Log.e(LOG_TAG, "Error getting document", task.getException());
                isReg.isRegistered(false);
            }
        });
    }


    private void uploadImageToStorage(Uri uri, getImageUrl uploadURL) {
        imageUploadFdbkTV.setText(R.string.uploading_image);
        // Upload the image to Firebase Storage
        StorageReference imageRef = storageRef.child(System.currentTimeMillis() + "_product-image.jpg");

        imageRef.putFile(uri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageUploadFdbkTV.setText(R.string.image_upload_success);
                    imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        uploadURL.onImageUploaded(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "error uploading image, try again!", Toast.LENGTH_SHORT).show();
                });
    }

    private void addNewProductToDB(View view, String imgURL, String uid) {

        dataMap.clear();
        dataMap1.clear();

        dataMap.put("item_image_url", imgURL);
        String itemName = addItemNameET.getText().toString();
        dataMap.put("item_id", Utils.generateItemID());
        dataMap.put("item_name", (Character.toUpperCase(itemName.charAt(0)) + itemName.substring(1)));
        dataMap.put("item_name_reference", addItemNameET.getText().toString().toLowerCase());
        dataMap.put("item_price", Integer.parseInt(String.valueOf(addItemPriceET.getText())));
        dataMap.put("item_price_unit", addItemPriceUnitET.getText().toString().toLowerCase());

        // Create a map to hold the entire document data
        dataMap1.put(addItemNameET.getText().toString().toLowerCase(), dataMap);

        retailerAddNewProductRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot documentSnapshot = task.getResult();
                if (documentSnapshot.exists()) {
                    retailerAddNewProductRef.update(dataMap1).addOnSuccessListener(var -> {
                                addProductBtn.setEnabled(true);
                                addProductBtn.setText(R.string.add_product);
                                selectedImageIV.setImageResource(R.drawable.baseline_add_a_photo_24);
                                selectedImageUri = null;
                                imageUploadFdbkTV.setText("");
                                Toast.makeText(view.getContext(), "Product updated to VID: " + uid, Toast.LENGTH_SHORT).show();
                            }
                    ).addOnFailureListener(e -> {
                        Log.d(LOG_TAG, "unable to add new product!");
                        Toast.makeText(view.getContext(), "Unable to add new product!", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    retailerAddNewProductRef.set(dataMap1).addOnSuccessListener(var -> {
                                Log.d(LOG_TAG, "new product added");
                                Toast.makeText(view.getContext(), "New Product added to VID: " + uid, Toast.LENGTH_SHORT).show();
                            }
                    ).addOnFailureListener(e -> {
                        Log.d(LOG_TAG, "unable to add new product!");
                        Toast.makeText(view.getContext(), "Unable to add new product!", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}