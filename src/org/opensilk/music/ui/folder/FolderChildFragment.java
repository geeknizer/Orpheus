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

package org.opensilk.music.ui.folder;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;

import com.andrew.apollo.R;

import org.opensilk.filebrowser.FileBrowserArgs;
import org.opensilk.filebrowser.FileItem;
import org.opensilk.filebrowser.FileItemArrayLoader;
import org.opensilk.music.ui.cards.FileItemCard;
import org.opensilk.music.ui.home.CardListGridFragment;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;

/**
 * Created by drew on 7/2/14.
 */
public class FolderChildFragment extends CardListGridFragment implements LoaderManager.LoaderCallbacks<List<FileItem>> {

    protected static final int LOADER = 0;

    protected DaggerInjector mInjector;
    private CardArrayAdapter mAdapter;
    private FileBrowserArgs mBrowserArgs;

    public static FolderChildFragment newInstance(FileBrowserArgs args) {
        FolderChildFragment f = new FolderChildFragment();
        Bundle b = new Bundle(1);
        b.putParcelable("__args", args);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mInjector = (DaggerInjector) getParentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBrowserArgs = getArguments().getParcelable("__args");
        mAdapter = new CardArrayAdapter(getActivity(), new ArrayList<Card>());
        // Start the loader
        getLoaderManager().initLoader(LOADER, null, this);
    }

    @Override
    //@DebugLog
    public void onViewCreated(View view, Bundle savedInstanceState) {
        setEmptyText(getEmptyText());
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(mAdapter);
        setListShown(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mInjector = null;
    }

    protected CharSequence getEmptyText() {
        return getString(R.string.empty_music);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<List<FileItem>> onCreateLoader(int id, Bundle args) {
        return new FileItemArrayLoader(getActivity(), mBrowserArgs);
    }

    @Override
    public void onLoadFinished(Loader<List<FileItem>> loader, List<FileItem> data) {
        mAdapter.clear();
        if (data != null && data.size() > 0) {
            List<Card> cards = new ArrayList<>(data.size());
            for (FileItem item : data) {
                FileItemCard c = new FileItemCard(getActivity(), item);
                mInjector.inject(c);
                cards.add(c);
            }
            mAdapter.addAll(cards);
        }
        setListShown(true);
    }

    @Override
    public void onLoaderReset(Loader<List<FileItem>> loader) {
        mAdapter.clear();
    }

    /*
     * Abstract methods
     */

    @Override
    public int getListViewLayout() {
        return R.layout.card_listview;
    }

    @Override
    public int getEmptyViewLayout() {
        return R.layout.list_empty_view;
    }

}