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

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui2.core.lifecycle.PauseAndResumeRegistrar;
import org.opensilk.music.ui2.core.lifecycle.PausesAndResumes;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.Observers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.music.util.RxUtil.isSubscribed;
import static org.opensilk.music.util.RxUtil.notSubscribed;

/**
 * Created by drew on 10/15/14.
 */
public class FooterScreen {

    @Singleton
    public static class Presenter extends ViewPresenter<FooterView> implements PausesAndResumes {

        final PauseAndResumeRegistrar pauseAndResumeRegistrar;
        final MusicServiceConnection musicService;

        CompositeSubscription broadcastSubscriptions;
        Subscription progressSubscription;

        Observable<Boolean> playStateObservable;
        Observable<String[]> metaObservable;
        Observable<ArtInfo> artworkObservable;
        Observable<Long[]> currentPositionObservable;

        Observer<Boolean> playStateObserver;
        Observer<String[]> metaObserver;
        Observer<ArtInfo> artworkObserver;
        Observer<Long[]> progressObserver;

        int progressWidth;

        @Inject
        public Presenter(PauseAndResumeRegistrar pauseAndResumeRegistrar,
                         MusicServiceConnection musicService) {
            this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
            this.musicService = musicService;
        }

        @Override
        protected void onEnterScope(MortarScope scope) {
            Timber.v("onEnterScope()");
            super.onEnterScope(scope);
            pauseAndResumeRegistrar.register(scope, this);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            //just for safety we should always receive a call to onPause()
            unsubscribeBroadcasts();
            unsubscribeProgress();
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad()");
            super.onLoad(savedInstanceState);
            setupObserables();
            setupObservers();
            progressWidth = getView().progressBar.getWidth();
            //just for safety we should always receive a call to onResume()
            subscribeBroadcasts();
            //playstate will kick off progress subscription
        }

        @Override
        public void onResume() {
            Timber.v("onResume()");
            if (getView() == null) return;
            subscribeBroadcasts();
            //playstate will kick off progress subscription
        }

        @Override
        public void onPause() {
            Timber.v("onPause");
            unsubscribeBroadcasts();
            unsubscribeProgress();
        }

        void setTrackName(String s) {
            FooterView v = getView();
            if (v == null) return;
            v.trackTitle.setText(s);
        }

        void setArtistName(String s) {
            FooterView v = getView();
            if (v == null) return;
            v.artistName.setText(s);
        }

        void setProgress(int progress) {
            FooterView v = getView();
            if (v == null) return;
            v.progressBar.setProgress(progress);
        }

        void updateArtwork(ArtInfo artInfo) {
            FooterView v = getView();
            if (v == null) return;
            ArtworkManager.loadImage(artInfo, v.artworkThumbnail);
        }

        void setupObserables() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
            intentFilter.addAction(MusicPlaybackService.META_CHANGED);
            Scheduler scheduler = Schedulers.computation();
            // obr will call onNext on the main thread so we observeOn computation
            // so our chained operators will be called on computation instead of main.
            Observable<Intent> intentObservable = AndroidObservable.fromBroadcast(getView().getContext(), intentFilter).observeOn(scheduler);
            playStateObservable = intentObservable
                    // Filter for only PLAYSTATE_CHANGED actions
                    .filter(new Func1<Intent, Boolean>() {
                        // called on computation
                        @Override
                        public Boolean call(Intent intent) {
                            Timber.v("playstateSubscripion filter called on %s", Thread.currentThread().getName());
                            return intent.getAction() != null && intent.getAction().equals(MusicPlaybackService.PLAYSTATE_CHANGED);
                        }
                    })
                    // filter out repeats only taking most recent
                    .debounce(20, TimeUnit.MILLISECONDS, scheduler)
                    // flatMap the intent into a boolean by requesting the playstate
                    // XXX the intent contains the playstate as an extra but
                    //     it could be out of date
                    .flatMap(new Func1<Intent, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(Intent intent) {
                            Timber.v("playstateSubscription flatMap called on %s", Thread.currentThread().getName());
                            return musicService.isPlaying();
                        }
                    })
                    // observe final result on main thread
                    .observeOn(AndroidSchedulers.mainThread());
            Observable<Intent> metaChangedObservable = intentObservable
                    // filter only the META_CHANGED actions
                    .filter(new Func1<Intent, Boolean>() {
                        // will be called on computation
                        @Override
                        public Boolean call(Intent intent) {
                            Timber.v("metaObservable(filter) %s", Thread.currentThread().getName());
                            return intent.getAction() != null && intent.getAction().equals(MusicPlaybackService.META_CHANGED);
                        }
                    })
                    // buffer quick successive calls and only emit the most recent
                    .debounce(20, TimeUnit.MILLISECONDS, scheduler);
            metaObservable = metaChangedObservable
                    // flatmap the intent into a String[] containing the trackname and artistname
                    // XXX these are included in the intent as extras but could be out of date
                    .flatMap(new Func1<Intent, Observable<String[]>>() {
                        // called on computation
                        @Override
                        public Observable<String[]> call(Intent intent) {
                            Timber.v("metaObservable(flatMap) %s", Thread.currentThread().getName());
                            return Observable.zip(musicService.getTrackName(), musicService.getArtistName(), new Func2<String, String, String[]>() {
                                // getTrackName and getArtistName will emit their values on IO threads so this gets called on an IO thread
                                // as a side note the getTrackName and getArtistName operate in parallel here
                                @Override
                                public String[] call(String trackName, String artistName) {
                                    Timber.v("metaObservable(zip) called on %s", Thread.currentThread().getName());
                                    return new String[]{trackName, artistName};
                                }
                            });
                        }
                    })
                    // we want the final value to come in on the main thread
                    .observeOn(AndroidSchedulers.mainThread());
            artworkObservable = metaChangedObservable
                    .flatMap(new Func1<Intent, Observable<ArtInfo>>() {
                        @Override
                        public Observable<ArtInfo> call(Intent intent) {
                            return musicService.getCurrentArtInfo();
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread());
            currentPositionObservable =
                    Observable.zip(musicService.getPosition(), musicService.getDuration(), new Func2<Long, Long, Long[]>() {
                        @Override
                        public Long[] call(Long position, Long duration) {
                            Timber.v("currentPositionObservable(%d, %d) %s", position, duration, Thread.currentThread().getName());
                            long progress;
                            if (position > 0 && duration > 0) {
                                progress = (1000 * position / duration);
                            } else {
                                progress = 1000;
                            }
                            int width = progressWidth;
                            if (width <= 0) {
                                width = 320;
                            }
                            long nextrefreshtime = duration / width;
                            if (nextrefreshtime < 20) {
                                nextrefreshtime = 50;
                            }
                            return new Long[] {progress, nextrefreshtime};
                        }
                    }).observeOn(AndroidSchedulers.mainThread());
        }

        void setupObservers() {
            playStateObserver = Observers.create(new Action1<Boolean>() {
                @Override
                public void call(Boolean playing) {
                    Timber.v("playStateObserver(result) %s", Thread.currentThread().getName());
                    if (playing) {
                        subscribeProgress(0);
                    } else {
                        unsubscribeProgress();
                        // update the current position
                        currentPositionObservable.observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Long[]>() {
                            @Override
                            public void call(Long[] longs) {
                                setProgress(longs[0].intValue());
                            }
                        });
                    }
                }
            });
            metaObserver = Observers.create(new Action1<String[]>() {
                @Override
                public void call(String[] strings) {
                    Timber.v("metaObserver(result) %s", Thread.currentThread().getName());
                    if (strings.length == 2) {
                        setTrackName(strings[0]);
                        setArtistName(strings[1]);
                    }
                }
            });
            artworkObserver = Observers.create(new Action1<ArtInfo>() {
                @Override
                public void call(ArtInfo artInfo) {
                    updateArtwork(artInfo);
                }
            });
            progressObserver = Observers.create(new Action1<Long[]>() {
                @Override
                public void call(Long[] values) {
//                    Timber.d("progressObserver(result) %s", Thread.currentThread().getName());
                    setProgress(values[0].intValue());
                    // resubscribe the next duration
                    unsubscribeProgress();
                    subscribeProgress(values[1]);
                }
            });
        }

        void subscribeBroadcasts() {
            if (notSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions = new CompositeSubscription(
                        playStateObservable.subscribe(playStateObserver),
                        metaObservable.subscribe(metaObserver),
                        artworkObservable.subscribe(artworkObserver)
                );
            }
        }

        void unsubscribeBroadcasts() {
            if (isSubscribed(broadcastSubscriptions)) {
                broadcastSubscriptions.unsubscribe();
                broadcastSubscriptions = null;
            }
        }

        void subscribeProgress(long delay) {
            Timber.d("subscribeProgress delay=%d", delay);
            if (notSubscribed(progressSubscription)) {
                progressSubscription = currentPositionObservable.delay(delay,
                        TimeUnit.MILLISECONDS).subscribe(progressObserver);
            }
        }

        void unsubscribeProgress() {
            if (isSubscribed(progressSubscription)) {
                progressSubscription.unsubscribe();
                progressSubscription = null;
            }
        }

    }
}
