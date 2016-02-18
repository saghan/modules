package org.motechproject.atomclient.service;

import com.rometools.fetcher.impl.FeedFetcherCache;
import com.rometools.fetcher.impl.SyndFeedInfo;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.SyndFeedOutput;
import org.apache.commons.lang.StringUtils;
import org.motechproject.atomclient.domain.FeedRecord;
import org.motechproject.atomclient.repository.FeedRecordDataService;
import org.motechproject.event.MotechEvent;
import org.motechproject.event.listener.EventRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeedCache implements FeedFetcherCache {


    public static final String FEED_DATA = "feedData";
    private FeedRecordDataService feedRecordDataService;
    private AtomClientConfigService atomClientConfigService;
    private EventRelay eventRelay;

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedCache.class);


    public FeedCache(FeedRecordDataService feedRecordDataService, EventRelay eventRelay,
                     AtomClientConfigService atomClientConfigService) {
        this.feedRecordDataService = feedRecordDataService;
        this.eventRelay = eventRelay;
        this.atomClientConfigService = atomClientConfigService;
    }


    public static String feedToString(SyndFeed feed) throws FeedException, UnsupportedEncodingException {
        SyndFeedOutput syndFeedOutput = new SyndFeedOutput();
        return syndFeedOutput.outputString(feed);
    }


    public static SyndFeed feedFromString(String xml) throws IOException, ClassNotFoundException, FeedException {
        SyndFeedInput syndFeedInput = new SyndFeedInput();
        return syndFeedInput.build(new StringReader(xml));
    }


    public static String urlToString(URL url) throws IOException {
        return url.toExternalForm();
    }


    public static URL urlFromString(String s) throws IOException {
        return new URL(s);
    }


    public SyndFeedInfo feedRecordToFeedInfo(FeedRecord record) throws IOException, ClassNotFoundException, FeedException {
        SyndFeedInfo info = new SyndFeedInfo();
        info.setUrl(urlFromString(record.getUrl()));
        info.setLastModified(record.getLastModified());
        info.setSyndFeed(feedFromString(record.getData()));
        return info;
    }


    /**
     * Get a SyndFeedInfo object from the cache.
     *
     * @param feedUrl The url of the feed
     * @return A SyndFeedInfo or null if it is not in the cache
     */
    @Override
    public SyndFeedInfo getFeedInfo(URL feedUrl) {
        try {
            String url = urlToString(feedUrl);
            FeedRecord record = feedRecordDataService.findByURL(url);
            if (record != null) {
                return feedRecordToFeedInfo(record);
            }
        } catch (IOException | ClassNotFoundException | FeedException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
        return null;
    }


    private Map<String, Object> extractContent(String content, String regex, String key) {
        Map<String, Object> map = new HashMap<>();

        if (StringUtils.isBlank(regex)) {
            return map;
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            if (matcher.groupCount() > 1) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    map.put(String.format("%s_%d", key, i), matcher.group(i));
                }
            } else if (matcher.groupCount() > 0) {
                map.put(key, matcher.group(1));
            }
        }

        return map;
    }


    /**
     * Sends a MOTECH event for a feed entry
     *
     * @param entry
     */
    private void sendMessageForFeedEntry(SyndEntry entry, String regex) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("uri", entry.getUri());
        if (entry.getContents() != null) {
            if (entry.getContents().size() == 1) {
                SyndContent content = entry.getContents().get(0);
                parameters.put("rawcontent", content.getValue());
                parameters.putAll(extractContent(content.getValue(), regex, "extractedContent"));
            } else {
                int index = 1;
                for (SyndContent content : entry.getContents()) {
                    parameters.put(String.format("rawcontent%d", index), content.getValue());
                    String key = String.format("extractedcontent%d", index);
                    parameters.putAll(extractContent(content.getValue(), regex, key));
                    index++;
                }
            }
        } else {
            LOGGER.warn("NULL content for entry {}", entry.getUri());
            parameters.put("content", null);
        }
        MotechEvent event = new MotechEvent(Constants.FEED_CHANGE_MESSAGE, parameters);
        LOGGER.debug("sending message {}", event);
        eventRelay.sendEventMessage(event);
    }


    /**
     * Sends a MOTECH event for each new feed entry
     *
     * @param feed
     */
    private void sendMessagesForNewFeedData(SyndFeed feed, String regex) {
        for (SyndEntry entry : feed.getEntries()) {
            sendMessageForFeedEntry(entry, regex);
        }
    }


    private static boolean areEntriesDifferent(SyndEntry e1, SyndEntry e2) {
        if (!e1.getUpdatedDate().equals(e2.getUpdatedDate())) {
            LOGGER.trace("different updated date");
            return true;
        }
        if (!e1.getPublishedDate().equals(e2.getPublishedDate())) {
            LOGGER.trace("different published date");
            return true;
        }
        if (!e1.getUri().equals(e2.getUri())) {
            LOGGER.trace("different uri");
            return true;
        }
        if (!e1.getContents().equals(e2.getContents())) {
            LOGGER.trace("different contents");
            return true;
        }

        return false;
    }


    /**
     * Sends a MOTECH event for each changed feed entry and returns true if changes were detected
     *
     * @param cachedFeed
     * @param fetchedFeed
     * @return true if any changes were detected between the cached entry and the fetched entry
     */
    private boolean sendMessagesForChangedEntries(SyndFeed cachedFeed, SyndFeed fetchedFeed, String regex) {
        boolean anyChanges = false;
        for (SyndEntry fetchedEntry : fetchedFeed.getEntries()) {
            boolean foundInCache = false;
            for (SyndEntry cachedEntry : cachedFeed.getEntries()) {
                if (fetchedEntry.getUri().equals(cachedEntry.getUri())) {
                    foundInCache = true;
                    if (areEntriesDifferent(fetchedEntry, cachedEntry)) {
                        sendMessageForFeedEntry(fetchedEntry, regex);
                        anyChanges = true;
                    }
                }
            }
            if (!foundInCache) {
                sendMessageForFeedEntry(fetchedEntry, regex);
                anyChanges = true;
            }
        }
        return anyChanges;
    }


    private FeedRecord recordFromFeed(String url, SyndFeedInfo info) throws IOException, FeedException {
        return new FeedRecord(url, (Long) info.getLastModified(), feedToString(info.getSyndFeed()));
    }


    /**
     * Add a SyndFeedInfo object to the cache
     *
     * @param feedUrl The url of the feed
     * @param feedInfo A SyndFeedInfo for the feed
     */
    @Override
    public void setFeedInfo(URL feedUrl, SyndFeedInfo feedInfo) {
        try {
            String url = urlToString(feedUrl);
            String regex = atomClientConfigService.getRegexForFeedUrl(url);
            FeedRecord record = feedRecordDataService.findByURL(url);
            if (record != null) {
                SyndFeedInfo cachedFeedInfo = feedRecordToFeedInfo(record);
                if (sendMessagesForChangedEntries(cachedFeedInfo.getSyndFeed(), feedInfo.getSyndFeed(), regex)) {
                    feedRecordDataService.delete(record);
                    feedRecordDataService.create(recordFromFeed(url, feedInfo));
                }
            } else {
                sendMessagesForNewFeedData(feedInfo.getSyndFeed(), regex);
                feedRecordDataService.create(recordFromFeed(url, feedInfo));
            }
        } catch (IOException | FeedException | ClassNotFoundException ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }


    /**
     * Removes all items from the cache.
     */
    @Override
    public void clear() {
        feedRecordDataService.deleteAll();
    }


    /**
     * Removes the SyndFeedInfo identified by the url from the cache.
     *
     * @return The removed SyndFeedInfo
     */
    @Override
    public SyndFeedInfo remove(URL feedUrl) {
        try {
            String url = urlToString(feedUrl);
            FeedRecord record = feedRecordDataService.findByURL(url);
            if (record == null) {
                LOGGER.error("Trying to remove feedRecord for inexistent URL {}", feedUrl);
                return null;
            }
            LOGGER.debug("*** removing from cache *** {}", url);
            byte[] bytes = (byte[]) feedRecordDataService.getDetachedField(record, FEED_DATA);
            feedRecordDataService.delete(record);
            return feedRecordToFeedInfo(record);
        } catch (IOException | ClassNotFoundException | FeedException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return null;
        }
    }
}
