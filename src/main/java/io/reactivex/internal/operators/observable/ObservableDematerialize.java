/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class ObservableDematerialize<T> extends ObservableWithUpstream<Try<Optional<T>>, T> {
    
    public ObservableDematerialize(ObservableSource<Try<Optional<T>>> source) {
        super(source);
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new DematerializeSubscriber<T>(t));
    }
    
    static final class DematerializeSubscriber<T> implements Observer<Try<Optional<T>>>, Disposable {
        final Observer<? super T> actual;
        
        boolean done;

        Disposable s;
        
        public DematerializeSubscriber(Observer<? super T> actual) {
            this.actual = actual;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                
                actual.onSubscribe(this);
            }
        }
        

        @Override
        public void dispose() {
            s.dispose();
        }
        
        @Override
        public boolean isDisposed() {
            return s.isDisposed();
        }

        
        @Override
        public void onNext(Try<Optional<T>> t) {
            if (done) {
                return;
            }
            if (t.hasError()) {
                s.dispose();
                onError(t.error());
            } else {
                Optional<T> o = t.value();
                if (o.isPresent()) {
                    actual.onNext(o.get());
                } else {
                    s.dispose();
                    onComplete();
                }
            }
        }
        
        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
                return;
            }
            done = true;
            
            actual.onError(t);
        }
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            
            actual.onComplete();
        }
    }
}