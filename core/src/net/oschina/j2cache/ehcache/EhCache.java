/**
 * Copyright (c) 2015-2017, Winter Lau (javayou@gmail.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oschina.j2cache.ehcache;

import java.io.Serializable;
import java.util.*;

import net.oschina.j2cache.CacheException;
import net.oschina.j2cache.CacheExpiredListener;
import net.oschina.j2cache.TTLEnableCache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

/**
 * <p>EHCache 2.x 的缓存封装</p>
 * <p>该封装类实现了缓存操作以及对缓存数据失效的侦听</p>
 *
 * @author Winter Lau(javayou@gmail.com)
 */
public class EhCache implements TTLEnableCache, CacheEventListener {
	
	private net.sf.ehcache.Cache cache;
	private CacheExpiredListener listener;

	/**
	 * Creates a new EhCache instance
	 *
	 * @param cache The underlying EhCache instance to use.
	 * @param listener cache listener
	 */
	public EhCache(net.sf.ehcache.Cache cache, CacheExpiredListener listener) {
		this.cache = cache;
		this.cache.getCacheEventNotificationService().registerListener(this);
		this.listener = listener;
	}

	public long getTimeToLiveSeconds() {
		return cache.getCacheConfiguration().getTimeToLiveSeconds();
	}

	@Override
	public Collection<String> keys() {
		return this.cache.getKeys();
	}

	/**
	 * Gets a value of an element which matches the given key.
	 *
	 * @param key the key of the element to return.
	 * @return The value placed into the cache with an earlier put, or null if not found or expired
	 */
	@Override
	public Serializable get(String key) {
		if ( key == null )
			return null;
		Element elem = cache.get( key );
		return (elem == null)?null:(Serializable)elem.getObjectValue();
	}

	/**
	 * Puts an object into the cache.
	 *
	 * @param key   a key
	 * @param value a value
	 */
	@Override
	public void put(String key, Serializable value) {
		put(key, value, 0);
	}

	/**
	 * Removes the element which matches the key
	 * If no element matches, nothing is removed and no Exception is thrown.
	 *
	 * @param keys the key of the element to remove
	 */
	@Override
	public void evict(String...keys) {
		try {
			cache.removeAll(Arrays.asList(keys));
		} catch (IllegalStateException | net.sf.ehcache.CacheException e) {
			throw new CacheException( e );
		}
	}

	@Override
	public Map getAll(Collection<String> keys) {
		return cache.getAll(keys);
	}

	@Override
	public boolean exists(String key) {
		return cache.isKeyInCache(key);
	}

	@Override
	public void putAll(Map<String, Serializable> elements) {
		List<Element> elems = new ArrayList<>();
		elements.forEach((k,v) -> elems.add(new Element(k,v)));
		cache.putAll(elems);
	}

	@Override
	public void put(String key, Serializable value, int timeToLiveInSeconds) {
		Element elem = new Element(key, value);
		if(timeToLiveInSeconds != 0) {
			elem.setTimeToIdle(timeToLiveInSeconds);
			elem.setTimeToLive(timeToLiveInSeconds);
		}
		cache.put(elem);
	}

	@Override
	public Serializable putIfAbsent(String key, Serializable value, int timeToLiveInSeconds) {
		Element obj = new Element(key, value);
		if(timeToLiveInSeconds != 0) {
			obj.setTimeToIdle(timeToLiveInSeconds);
			obj.setTimeToLive(timeToLiveInSeconds);
		}
		Element elem = cache.putIfAbsent(obj);
		return (elem!=null)?(Serializable)elem.getObjectValue():null;
	}

	@Override
	public void putAll(Map<String, Serializable> elements, int timeToLiveInSeconds) {
		List<Element> elems = new ArrayList<>();
		elements.forEach((k,v) -> {
			Element elem = new Element(k, v);
			if(timeToLiveInSeconds != 0) {
				elem.setTimeToIdle(timeToLiveInSeconds);
				elem.setTimeToLive(timeToLiveInSeconds);
			}
			elems.add(elem);
		});
		cache.putAll(elems);
	}

	/**
	 * Remove all elements in the cache, but leave the cache
	 * in a useable state.
	 */
	public void clear() {
		cache.removeAll();
	}

	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public void notifyElementExpired(Ehcache cache, Element elem) {
		if(listener != null){
			listener.notifyElementExpired(cache.getName(), (String)elem.getObjectKey());
		}
	}

	@Override
	public void notifyElementEvicted(Ehcache cache, Element elem) {}

	@Override
	public void notifyElementPut(Ehcache cache, Element elem) {}

	@Override
	public void notifyElementRemoved(Ehcache cache, Element elem) {}

	@Override
	public void notifyElementUpdated(Ehcache cache, Element elem) {}

	@Override
	public void notifyRemoveAll(Ehcache cache) {}

	@Override
	public void dispose() {}

}