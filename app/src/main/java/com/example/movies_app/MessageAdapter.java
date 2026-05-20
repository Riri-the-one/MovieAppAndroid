package com.example.movies_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter pour afficher les bulles de conversation utilisateur / bot.
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;

    private final List<Message> messages = new ArrayList<>();
    private RecyclerView recyclerView;

    /** Lie le RecyclerView pour défiler automatiquement vers le dernier message. */
    public void attachRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layoutRes = viewType == VIEW_TYPE_USER
                ? R.layout.item_message_user
                : R.layout.item_message_bot;
        View view = inflater.inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.textMessage.setText(message.getText());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    /** Ajoute un message, met à jour la liste et fait défiler vers le bas. */
    public void addMessage(Message message) {
        messages.add(message);
        int position = messages.size() - 1;
        notifyItemInserted(position);
        scrollToBottom(position);
    }

    /** Retire le dernier message (ex. message de chargement temporaire). */
    public void removeLastMessage() {
        if (messages.isEmpty()) {
            return;
        }
        int position = messages.size() - 1;
        messages.remove(position);
        notifyItemRemoved(position);
    }

    private void scrollToBottom(int position) {
        if (recyclerView != null) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(position));
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        final TextView textMessage;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
        }
    }
}
