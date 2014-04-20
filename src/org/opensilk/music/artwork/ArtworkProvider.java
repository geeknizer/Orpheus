/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.artwork;

import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.andrew.apollo.BuildConfig;

import java.io.FileNotFoundException;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/25/14.
 */
public class ArtworkProvider extends ContentProvider implements ServiceConnection {
    private static final String TAG = ArtworkProvider.class.getSimpleName();

    private static final String AUTHORITY = BuildConfig.ARTWORK_AUTHORITY;
    private static final UriMatcher sUriMatcher;

    public static final Uri ARTWORK_URI;
    public static final Uri ARTWORK_THUMB_URI;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        ARTWORK_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("artwork").build();
        sUriMatcher.addURI(AUTHORITY, "artwork/#", 1);

        ARTWORK_THUMB_URI = new Uri.Builder().scheme("content").authority(AUTHORITY).appendPath("thumbnail").build();
        sUriMatcher.addURI(AUTHORITY, "thumbnail/#", 2);
    }

    /**
     * @param albumId
     * @return Uri to retrieve large (fullscreen) artwork for specified albumId
     */
    public static Uri createArtworkUri(final long albumId) {
        return ARTWORK_URI.buildUpon().appendPath(String.valueOf(albumId)).build();
    }

    /**
     * @param albumId
     * @return Uri to retrieve thumbnail for specified albumId
     */
    public static Uri createArtworkThumbnailUri(final long albumId) {
        return ARTWORK_THUMB_URI.buildUpon().appendPath(String.valueOf(albumId)).build();
    }

    /**
     * Artwork service connection, we proxy requests to the image cache through here
     */
    private IArtworkServiceImpl mArtworkService;

    @Override
    @DebugLog
    public boolean onCreate() {
        doBindService();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                return "image/*";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Provider is read only");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode, CancellationSignal signal) throws FileNotFoundException {
        return openFile(uri, mode);
    }

    @Override
    @DebugLog
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new IllegalArgumentException("Provider is read only");
        }
        if (mArtworkService == null) {
            waitForService();
        }
        switch (sUriMatcher.match(uri)) {
            case 1: //Fullscreen
                try {
                    final long id = Long.decode(uri.getLastPathSegment());
                    final ParcelFileDescriptor pfd = mArtworkService.getArtwork(id);
                    if (pfd != null) {
                        return pfd;
                    }
                } catch (RemoteException e) {
                    throw new FileNotFoundException("" + e.getClass().getName() + " " + e.getMessage());
                }
                break;
            case 2: //Thumbnail
                try {
                    final long id = Long.decode(uri.getLastPathSegment());
                    final ParcelFileDescriptor pfd = mArtworkService.getArtworkThumbnail(id);
                    if (pfd != null) {
                        return pfd;
                    }
                } catch (RemoteException e) {
                    throw new FileNotFoundException("" + e.getClass().getName() + " " + e.getMessage());
                }
                break;
        }
        throw new FileNotFoundException("Could not obtain image from cache");
    }

    /**
     * Waits for service to bind
     * @throws FileNotFoundException
     */
    private synchronized void waitForService() throws FileNotFoundException {
        try {
            long waitTime = 0;
            while (mArtworkService == null) {
                // Don' block for more than a second
                if (waitTime > 1000) throw new FileNotFoundException("Could not bind service");
                Log.i(TAG, "Waiting on service");
                // We were called too soon after onCreate, give the service some time
                // to spin up, This is run in a binder thread so it shouldn't be a big deal
                // to block it.
                long start = System.currentTimeMillis();
                wait(100);
                Log.i(TAG, "Waited for " + (waitTime += (System.currentTimeMillis() - start)) + "ms");
            }
        } catch (InterruptedException e) {
            throw new FileNotFoundException(""+e.getClass().getName() + " " + e.getMessage());
        }
    }

    /**
     * Tries to bind to the artwork service
     */
    private void doBindService() {
        final Context ctx = getContext();
        if (ctx != null) {
            ctx.bindService(new Intent(ctx, ArtworkService.class), this, Context.BIND_AUTO_CREATE);
        }
    }

    /*
     * Implement ServiceConnection interface
     */

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        mArtworkService = (IArtworkServiceImpl) IArtworkServiceImpl.asInterface(service);
        notifyAll();
    }

    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
        mArtworkService = null;
        notifyAll();
        doBindService();
    }

}
