
package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;
import com.glaciersecurity.glaciermessenger.ui.adapter.CallParticipantsPage;
import com.glaciersecurity.glaciermessenger.ui.CallParticipantView;
import com.glaciersecurity.glaciermessenger.ui.CallParticipantsLayout;
import com.glaciersecurity.glaciermessenger.R;
//AM-558


public class CallParticipantsPagerAdapter extends ListAdapter<CallParticipantsPage, CallParticipantsPagerAdapter.ViewHolder> {

  private static final int VIEW_TYPE_MULTI  = 0;
  private static final int VIEW_TYPE_SINGLE = 1;

  //private final Runnable onPageClicked;

  public CallParticipantsPagerAdapter(@NonNull Runnable onPageClicked) {
    super(new DiffCallback());
    //null for now so commeted out below
    //this.onPageClicked = onPageClicked;
  }

  @Override
  public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
    recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
  }

  @Override
  public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    final ViewHolder viewHolder;

    switch (viewType) {
      case VIEW_TYPE_SINGLE:
        viewHolder = new SingleParticipantViewHolder((CallParticipantView) LayoutInflater.from(parent.getContext())
            .inflate(R.layout.call_participant_item,
                parent,
                false));
        break;
      case VIEW_TYPE_MULTI:
        viewHolder = new MultipleParticipantViewHolder((CallParticipantsLayout) LayoutInflater.from(parent.getContext())
            .inflate(R.layout.webrtc_call_participants_layout,
                parent,
                false));
        break;
      default:
        throw new IllegalArgumentException("Unsupported viewType: " + viewType);
    }

    //viewHolder.itemView.setOnClickListener(unused -> onPageClicked.run());

    return viewHolder;
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  @Override
  public int getItemViewType(int position) {
    return VIEW_TYPE_MULTI;
  }

  static abstract class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(@NonNull View itemView) {
      super(itemView);
    }

    abstract void bind(CallParticipantsPage page);
  }

  private static class MultipleParticipantViewHolder extends ViewHolder {

    private final CallParticipantsLayout callParticipantsLayout;

    private MultipleParticipantViewHolder(@NonNull CallParticipantsLayout callParticipantsLayout) {
      super(callParticipantsLayout);
      this.callParticipantsLayout = callParticipantsLayout;
    }

    @Override
    void bind(CallParticipantsPage page) {
      callParticipantsLayout.update(page.getCallParticipants());
    }
  }

  private static class SingleParticipantViewHolder extends ViewHolder {

    private final CallParticipantView callParticipantView;

    private SingleParticipantViewHolder(CallParticipantView callParticipantView) {
      super(callParticipantView);
      this.callParticipantView = callParticipantView;

      ViewGroup.LayoutParams params = callParticipantView.getLayoutParams();

      params.height = ViewGroup.LayoutParams.MATCH_PARENT;
      params.width  = ViewGroup.LayoutParams.MATCH_PARENT;

      callParticipantView.setLayoutParams(params);
    }


    @Override
    void bind(CallParticipantsPage page) {
      callParticipantView.setCallParticipant(page.getCallParticipants().get(0));
      //callParticipantView.setRenderInPip(page.isRenderInPip());
    }
  }

  private static final class DiffCallback extends DiffUtil.ItemCallback<CallParticipantsPage> {
    @Override
    public boolean areItemsTheSame(@NonNull CallParticipantsPage oldItem, @NonNull CallParticipantsPage newItem) {
      return oldItem.equals(newItem);
    }

    @Override
    public boolean areContentsTheSame(@NonNull CallParticipantsPage oldItem, @NonNull CallParticipantsPage newItem) {
      return oldItem.equals(newItem);
    }
  }

}