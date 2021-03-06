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

package org.opensilk.music.api.meta;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

/**
 * Created by drew on 6/14/14.
 */
public class PluginInfo implements Parcelable, Comparable<PluginInfo> {

    /**
     * Manifest attribute android:label
     */
    public final CharSequence title;
    /**
     * Manifest attribute android:description
     */
    public final CharSequence description;
    /**
     * Manifest attribute android:name
     */
    public final ComponentName componentName;
    /**
     * Manifest attribute android:icon
     */
    public transient Drawable icon;
    /**
     * Internal use true if plugin shows in drawer
     */
    public transient boolean isActive = true;
    /**
     * Whether or not this library is protected by one of
     * {@link org.opensilk.music.api.OrpheusApi#PERMISSION_BIND_LIBRARY_SERVICE}
     * if not defined Orpheus will refuse to bind the service.
     */
    public boolean hasPermission = false;


    public PluginInfo(CharSequence title, CharSequence description, ComponentName componentName) {
        this.title = title;
        this.description = description;
        if (componentName != null) {
            this.componentName = componentName;
        } else {
            throw new NullPointerException("componentName must not be null");
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((componentName == null) ? 0 : componentName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null || !(o instanceof PluginInfo)) return false;
        PluginInfo p = (PluginInfo) o;
        if (!TextUtils.equals(p.title, this.title)) return false;
        if (!TextUtils.equals(p.description, this.description)) return false;
        if (!p.componentName.equals(this.componentName)) return false;
        return true;
    }

    @Override
    public String toString() {
        return title.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title.toString());
        dest.writeString(description.toString());
        dest.writeString(componentName.flattenToString());
    }

    private static PluginInfo readParcel(Parcel in) {
        return new PluginInfo(
                in.readString(),
                in.readString(),
                ComponentName.unflattenFromString(in.readString())
        );
    }

    public static final Creator<PluginInfo> CREATOR = new Creator<PluginInfo>() {
        @Override
        public PluginInfo createFromParcel(Parcel source) {
            return readParcel(source);
        }

        @Override
        public PluginInfo[] newArray(int size) {
            return new PluginInfo[size];
        }
    };

    @Override
    public int compareTo(@NonNull PluginInfo another) {
        return this.title.toString().compareTo(another.title.toString());
    }

}
