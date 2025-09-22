package com.saikishor.vivocamera;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaCodec;
import android.net.Uri;
import java.io.File;
import java.nio.ByteBuffer;

public class VideoEncoder {

    public static void speedUpVideo(File inputFile, File outputFile, float speedMultiplier) {
        try {
            MediaExtractor extractor = new MediaExtractor();
            extractor.setDataSource(inputFile.getAbsolutePath());

            int videoTrackIndex = -1;
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat f = extractor.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    videoTrackIndex = i;
                    format = f;
                    break;
                }
            }

            if (videoTrackIndex < 0) return;

            extractor.selectTrack(videoTrackIndex);

            MediaMuxer muxer = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int muxerVideoTrack = muxer.addTrack(format);
            muxer.start();

            ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

            while (true) {
                int sampleSize = extractor.readSampleData(buffer, 0);
                if (sampleSize < 0) break;

                info.offset = 0;
                info.size = sampleSize;
                info.presentationTimeUs = (long) (extractor.getSampleTime() / speedMultiplier);
                info.flags = extractor.getSampleFlags();

                muxer.writeSampleData(muxerVideoTrack, buffer, info);
                extractor.advance();
            }

            muxer.stop();
            muxer.release();
            extractor.release();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
