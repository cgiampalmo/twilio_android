package com.glaciersecurity.glaciermessenger.ui.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import com.glaciersecurity.glaciermessenger.utils.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.glaciersecurity.glaciermessenger.Config;
import com.glaciersecurity.glaciermessenger.utils.MimeUtils;

public class Attachment implements Parcelable {

    Attachment(Parcel in) {
        uri = in.readParcelable(Uri.class.getClassLoader());
        mime = in.readString();
        uuid = UUID.fromString(in.readString());
        type = Type.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(uri, flags);
        dest.writeString(mime);
        dest.writeString(uuid.toString());
        dest.writeString(type.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    public String getMime() {
        return mime;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        FILE, IMAGE, LOCATION, RECORDING
    }

    private final Uri uri;
    private final Type type;
    private final UUID uuid;
    private final String mime;

    private Attachment(UUID uuid, Uri uri, Type type, String mime) {
        this.uri = uri;
        this.type = type;
        this.mime = mime;
        this.uuid = uuid;
    }

    private Attachment(Uri uri, Type type, String mime) {
        this.uri = uri;
        this.type = type;
        this.mime = mime;
        this.uuid = UUID.randomUUID();
    }

    public static List<Attachment> of(final Context context, Uri uri, Type type) {
        final String mime = type == Type.LOCATION ?null :MimeUtils.guessMimeTypeFromUri(context, uri);
        return Collections.singletonList(new Attachment(uri, type, mime));
    }

    public static List<Attachment> of(final Context context, List<Uri> uris) {
        List<Attachment> attachments = new ArrayList<>();
        for(Uri uri : uris) {
            final String mime = MimeUtils.guessMimeTypeFromUri(context, uri);
            attachments.add(new Attachment(uri, mime != null && mime.startsWith("image/") ? Type.IMAGE : Type.FILE,mime));
        }
        return attachments;
    }

    public static Attachment of(UUID uuid, final File file, String mime) {
        return new Attachment(uuid, Uri.fromFile(file),mime != null && (mime.startsWith("image/") || mime.startsWith("video/")) ? Type.IMAGE : Type.FILE, mime);
    }

    public static List<Attachment> extractAttachments(final Context context, final Intent intent, Type type) {
        List<Attachment> uris = new ArrayList<>();
        if (intent == null) {
            return uris;
        }

        final String contentType = intent.getType();
        final Uri data = intent.getData();
        if (data == null) {
            final ClipData clipData = intent.getClipData();
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); ++i) {
                    final Uri uri = clipData.getItemAt(i).getUri();
                    Log.d(Config.LOGTAG,"uri="+uri+" contentType="+contentType);
                    final String mime = MimeUtils.guessMimeTypeFromUriAndMime(context, uri, contentType);
                    Log.d(Config.LOGTAG,"mime="+mime);
                    uris.add(new Attachment(uri, type, mime));
                }
            }
        } else {
            final String mime = MimeUtils.guessMimeTypeFromUriAndMime(context, data, contentType);
            uris.add(new Attachment(data, type, mime));
        }
        return uris;
    }

    //ALF AM-277
    public static List<Attachment> extractFileSafeAttachments(final Context context, final Intent intent, Type type) {
        List<Attachment> uris = new ArrayList<>();
        if (intent == null) {
            return uris;
        }

        final ClipData clipData = intent.getClipData();
        final String contentType = intent.getType();
        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); ++i) {
                final Uri uri = clipData.getItemAt(i).getUri();
                Log.d(Config.LOGTAG,"uri="+uri+" contentType="+contentType);
                final String mime = MimeUtils.guessMimeTypeFromUriAndMime(context, uri, contentType);
                Log.d(Config.LOGTAG,"mime="+mime);
                uris.add(new Attachment(uri, type, mime));
            }
        } else {
            final Uri data = intent.getData();
            final String mime = MimeUtils.guessMimeTypeFromUriAndMime(context, data, contentType);
            uris.add(new Attachment(data, type, mime));
        }

        return uris;
    }

    public boolean renderThumbnail() {
        return type == Type.IMAGE || (type == Type.FILE && mime != null && (mime.startsWith("video/") || mime.startsWith("image/")));
    }

    public Uri getUri() {
        return uri;
    }

    public UUID getUuid() {
        return uuid;
    }
}
