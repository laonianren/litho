/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.litho.widget;

import androidx.annotation.VisibleForTesting;
import androidx.collection.LruCache;
import com.facebook.litho.Size;
import javax.annotation.Nullable;

/**
 * Pool for pre-computing and storing ComponentTrees. Can be used to pre-compute and store
 * ComponentTrees before they are inserted in a RecyclerBinder. The RecyclerBinder will fetch the
 * precomputed ComponentTree from the pool if it's available instead of computing a layout
 * calculation when the item needs to be computed.
 */
public class ComponentWarmer {

  public static final String COMPONENT_WARMER_TAG = "component_warmer_tag";
  public static final int DEFAULT_MAX_SIZE = 10;

  public interface ComponentTreeHolderPreparer {

    /**
     * Create a ComponentTreeHolder instance from an existing render info which will be used as an
     * item in the underlying adapter of the RecyclerBinder
     */
    ComponentTreeHolder create(ComponentRenderInfo renderInfo);

    /**
     * Triggers a synchronous layout calculation for the ComponentTree held by the provided
     * ComponentTreeHolder.
     */
    void prepareSync(ComponentTreeHolder holder, @Nullable Size size);

    /**
     * Triggers an asynchronous layout calculation for the ComponentTree held by the provided
     * ComponentTreeHolder.
     */
    void prepareAsync(ComponentTreeHolder holder);
  }

  public interface Cache {
    @Nullable
    ComponentTreeHolder remove(String tag);

    void put(String tag, ComponentTreeHolder holder);

    @Nullable
    ComponentTreeHolder get(String tag);

    void evictAll();
  }

  private static class DefaultCache implements Cache {
    private final LruCache<String, ComponentTreeHolder> cache;

    DefaultCache(int maxSize) {
      cache = new LruCache<>(maxSize);
    }

    @Override
    public @Nullable ComponentTreeHolder remove(String tag) {
      return cache.remove(tag);
    }

    @Override
    public void put(String tag, ComponentTreeHolder holder) {
      cache.put(tag, holder);
    }

    @Override
    @Nullable
    public ComponentTreeHolder get(String tag) {
      return cache.get(tag);
    }

    @Override
    public void evictAll() {
      cache.evictAll();
    }
  }

  private final Cache mCache;
  private final ComponentTreeHolderPreparer mFactory;

  /**
   * Creates a ComponentWarmer for this RecyclerBinder. This ComponentWarmer instance will use the
   * same ComponentTree factory as the RecyclerBinder. The RecyclerBinder will query the
   * ComponentWarmer for cached items before creating new ComponentTrees. Uses a {@link LruCache} to
   * manage the internal cache.
   */
  public ComponentWarmer(RecyclerBinder recyclerBinder) {
    this(recyclerBinder, null);
  }

  /**
   * Same as {@link #ComponentWarmer(RecyclerBinder)} but uses the passed in Cache instance to
   * manage the internal cache.
   */
  public ComponentWarmer(RecyclerBinder recyclerBinder, @Nullable Cache cache) {
    this(recyclerBinder.getComponentTreeHolderPreparer(), cache);
    recyclerBinder.setComponentWarmer(this);
  }

  /**
   * Creates a ComponentWarmer which will use the provided ComponentTreeHolderPreparer instance to
   * create ComponentTreeHolder instances for preparing and caching items. Uses a {@link LruCache}
   * to manage the internal cache.
   */
  public ComponentWarmer(ComponentTreeHolderPreparer factory) {
    this(factory, null);
  }

  /**
   * Same as {@link #ComponentWarmer(ComponentTreeHolderPreparer)} but uses the passed in Cache
   * instance to manage the internal cache.
   */
  public ComponentWarmer(ComponentTreeHolderPreparer factory, @Nullable Cache cache) {
    if (factory == null) {
      throw new NullPointerException("factory == null");
    }

    mFactory = factory;
    mCache = cache == null ? new DefaultCache(DEFAULT_MAX_SIZE) : cache;
  }

  /**
   * Synchronously prepare the ComponentTree for the given ComponentRenderInfo.
   *
   * @param tag Set a tag on the prepared ComponentTree so it can be retrieved from cache.
   * @param componentRenderInfo to be prepared.
   * @param size if not null, it will have the size result at the end of computing the layout.
   *     Prepare calls which require a size result to be computed cannot be cancelled (@see {@link
   *     #cancelPrepare(String)}).
   */
  public void prepare(String tag, ComponentRenderInfo componentRenderInfo, @Nullable Size size) {
    final ComponentTreeHolder holder = mFactory.create(componentRenderInfo);
    mCache.put(tag, holder);
    mFactory.prepareSync(holder, size);
  }

  public void prepareAsync(String tag, ComponentRenderInfo componentRenderInfo) {
    final ComponentTreeHolder holder = mFactory.create(componentRenderInfo);
    mFactory.prepareAsync(holder);
    mCache.put(tag, holder);
  }

  public void evictAll() {
    mCache.evictAll();
  }

  public void remove(String tag) {
    mCache.remove(tag);
  }

  /**
   * If it exists, it returns the cached ComponentTreeHolder for this tag and removes it from cache.
   */
  @Nullable
  public ComponentTreeHolder consume(String tag) {
    return mCache.remove(tag);
  }

  /**
   * Cancels the prepare execution for the item with the given tag if it's currently running and it
   * removes the item from the cache.
   */
  public void cancelPrepare(String tag) {
    final ComponentTreeHolder holder = mCache.remove(tag);
    if (holder == null || holder.getComponentTree() == null) {
      return;
    }

    holder.getComponentTree().cancelLayoutAndReleaseTree();
  }

  @VisibleForTesting
  @Nullable
  ComponentTreeHolderPreparer getFactory() {
    return mFactory;
  }
}
