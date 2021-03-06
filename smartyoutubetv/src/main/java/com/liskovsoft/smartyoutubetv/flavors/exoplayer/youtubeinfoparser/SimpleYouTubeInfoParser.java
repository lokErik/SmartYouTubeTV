package com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser;

import android.net.Uri;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.mpdbuilder.MyMPDBuilder;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.misc.YouTubeGenericInfo;
import com.liskovsoft.smartyoutubetv.flavors.exoplayer.youtubeinfoparser.parser.misc.YouTubeMediaItem;
import com.liskovsoft.smartyoutubetv.misc.Helpers;

import java.io.InputStream;

public class SimpleYouTubeInfoParser implements YouTubeInfoParser {
    private final String[] mContent;

    private class MergeMediaVisitor extends YouTubeInfoVisitor {
        private final String mType;
        private final OnMediaFoundCallback mMediaFoundCallback;
        private MyMPDBuilder mMPDBuilder;
        private int mCounter = 1;
        private YouTubeGenericInfo mInfo;
        private Uri mHlsUrl;

        public MergeMediaVisitor(OnMediaFoundCallback mediaFoundCallback) {
            this(null, mediaFoundCallback);
        }

        public MergeMediaVisitor(String type, OnMediaFoundCallback mediaFoundCallback) {
            mType = type;
            mMediaFoundCallback = mediaFoundCallback;
        }

        @Override
        public void onGenericInfo(YouTubeGenericInfo info) {
            if (mMPDBuilder == null)
                mMPDBuilder = new MyMPDBuilder(info);

            mInfo = info;
        }

        @Override
        public void onMediaItem(YouTubeMediaItem mediaItem) {
            if (mediaItem.belongsToType(mType)) {
                mMPDBuilder.append(mediaItem);
            }
        }

        @Override
        public void onLiveItem(Uri hlsUrl) {
            mHlsUrl = hlsUrl;
        }

        @Override
        public void doneVisiting() {
            if (mCounter != mContent.length) {
                mCounter++;
                return;
            }

            // callback on the last loop (resolve problems with async processing)

            if (mInfo != null)
                mMediaFoundCallback.onInfoFound(mInfo);
            if (mHlsUrl != null)
                mMediaFoundCallback.onLiveUrlFound(mHlsUrl);
            if (!mMPDBuilder.isEmpty())
                mMediaFoundCallback.onDashMPDFound(mMPDBuilder.build());
        }
    }

    public SimpleYouTubeInfoParser(InputStream ...content) {
        mContent = new String[content.length];
        readContent(content);
    }

    private void readContent(InputStream[] content) {
        for (int i = 0; i < content.length; i++) {
            mContent[i] = Helpers.toString(content[i]);
        }
    }

    @Override
    public void setOnMediaFoundCallback(OnMediaFoundCallback mpdFoundCallback) {
        YouTubeInfoVisitor visitor = new MergeMediaVisitor(mpdFoundCallback);

        for (String content : mContent) {
            YouTubeInfoVisitable visitable = new SimpleYouTubeInfoVisitable(content);
            visitable.accept(visitor);
        }

    }
}
