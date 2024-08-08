package com.vishnu.quickgoorder.server.ws;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vishnu.quickgoorder.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private final List<ChatModel> messageList;

    public ChatAdapter(List<ChatModel> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 1) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatModel message = messageList.get(position);
        holder.messageNameTV.setText(message.messageId());
        holder.messageTV.setText(message.content());
        holder.messageTimeTV.setText(message.messageTime());
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).isSent() ? 1 : 0;
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageNameTV;
        TextView messageTV;
        TextView messageTimeTV;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageNameTV = itemView.findViewById(R.id.messageNameView_textView);
            messageTV = itemView.findViewById(R.id.messageTextView_textView);
            messageTimeTV = itemView.findViewById(R.id.messageTimeView_textView);
        }
    }
}
