package com.glaciersecurity.glaciermessenger.ui.adapter;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.glaciersecurity.glaciermessenger.entities.TwilioCallParticipant;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
//AM-558

public class CallParticipantsPage {

  private final List<TwilioCallParticipant> callParticipants;
  
  public static CallParticipantsPage forMultipleParticipants(@NonNull List<TwilioCallParticipant> callParticipants)
  {
    return new CallParticipantsPage(callParticipants);
  }
  
  public static CallParticipantsPage forSingleParticipant(@NonNull TwilioCallParticipant singleParticipant)
  {
    return new CallParticipantsPage(Collections.singletonList(singleParticipant));
  }

  private CallParticipantsPage(@NonNull List<TwilioCallParticipant> callParticipants)
  {
    this.callParticipants   = callParticipants;
  }

  public @NonNull List<TwilioCallParticipant> getCallParticipants() {
    return callParticipants;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CallParticipantsPage that = (CallParticipantsPage) o;
    return callParticipants.equals(that.callParticipants);
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT)
  @Override
  public int hashCode() {
    return callParticipants.hashCode();
  }
}
