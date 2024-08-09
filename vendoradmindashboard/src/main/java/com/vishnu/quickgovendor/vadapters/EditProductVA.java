package com.vishnu.quickgovendor.vadapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.vishnu.quickgovendor.R;
import com.vishnu.quickgovendor.vmodels.EditProductVM;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EditProductVA extends RecyclerView.Adapter<EditProductVA.ViewHolder> {
    private final List<EditProductVM> itemList;
    private final Drawable drawableUp;
    private final Drawable drawableDwn;
    private final ProgressBar lpb;
    private final TextView ltv;
    private final String shopID;
    private final Context context;
    private boolean isCollapsed = true;
    private boolean onceExecute = true;


    public EditProductVA(List<EditProductVM> itemList, Context context, TextView ltv, ProgressBar lpb) {
        this.itemList = itemList;
        this.ltv = ltv;
        this.lpb = lpb;
        this.context = context;


        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        shopID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        drawableUp = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_drop_up_24);
        drawableDwn = ContextCompat.getDrawable(context, R.drawable.baseline_arrow_drop_down_24);
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.srv_edit_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EditProductVM editProductVM = itemList.get(position);

        Picasso.get().load(editProductVM.getImageUrl()).into(holder.itemImageView);
        holder.itemNameTV.setText(editProductVM.getItemDescp());
        holder.itemPriceTV.setText(MessageFormat.format("Rs: {0}", editProductVM.getItemPrice()));

        holder.showEditOptionTV.setOnClickListener(v -> toggleCollapse(holder.editOptionContainer, holder.showEditOptionTV, drawableUp, drawableDwn));

        holder.deleteItemBtn.setOnClickListener(v -> {
            removeFieldFromDocument(editProductVM.getItemNameRef());
            Toast.makeText(context, "Successfully deleted item: " + editProductVM.getItemDescp().toLowerCase(), Toast.LENGTH_SHORT).show();

        });

        if (onceExecute) {
            ltv.setVisibility(View.INVISIBLE);
            lpb.setVisibility(View.INVISIBLE);
            onceExecute = false;
        }
    }

    private void removeFieldFromDocument(String fieldName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("ShopData").document("itemData")
                .collection("allAvailableItems").document(shopID);

        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, FieldValue.delete());

        // Update the document to remove fieldMap
        docRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // mp2 entry removal successful

                })
                .addOnFailureListener(e -> {
                    // Handle failure
                });
    }

    private void toggleCollapse(LinearLayout lc, TextView se, Drawable dup, Drawable ddn) {
        // Toggle the collapse state
        isCollapsed = !isCollapsed;

        // Update the height of the LinearLayout
        ViewGroup.LayoutParams layoutParams = lc.getLayoutParams();
        if (isCollapsed) {
            layoutParams.height = 1;
            se.setText(R.string.show_edit_options);
            se.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, ddn, null);
        } else {
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            se.setText(R.string.hide_edit_options);
            se.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, dup, null);
        }
        lc.setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImageView;
        TextView itemNameTV;
        TextView itemPriceTV;
        Button deleteItemBtn;
        TextView showEditOptionTV;
        ImageButton okBtn;
        LinearLayout editOptionContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImageView = itemView.findViewById(R.id.srvItemImageEditProduct_imageView);
            itemNameTV = itemView.findViewById(R.id.srvItemNameEditProductView_textView);
            itemPriceTV = itemView.findViewById(R.id.srvItemCurrentPriceEditProduct_textView);
            showEditOptionTV = itemView.findViewById(R.id.showEditOptions_textView);
            editOptionContainer = itemView.findViewById(R.id.editOptionsContainer_linearLayout);
            deleteItemBtn = itemView.findViewById(R.id.srvDeleteEditProduct_button);
            okBtn = itemView.findViewById(R.id.srvOkEditProduct_imageButton);
        }
    }
}

