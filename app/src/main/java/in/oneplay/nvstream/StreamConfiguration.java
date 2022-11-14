package in.oneplay.nvstream;

import in.oneplay.nvstream.http.NvApp;
import in.oneplay.nvstream.jni.MoonBridge;

public class StreamConfiguration {
    public static final int INVALID_APP_ID = 0;

    public static final int STREAM_CFG_LOCAL = 0;
    public static final int STREAM_CFG_REMOTE = 1;
    public static final int STREAM_CFG_AUTO = 2;
    
    private NvApp app;
    private int width, height;
    private int refreshRate;
    private int launchRefreshRate;
    private int clientRefreshRateX100;
    private int bitrate;
    private boolean sops;
    private boolean enableAdaptiveResolution;
    private boolean playLocalAudio;
    private int maxPacketSize;
    private int remote;
    private MoonBridge.AudioConfiguration audioConfiguration;
    private boolean supportsHevc;
    private int hevcBitratePercentageMultiplier;
    private boolean enableHdr;
    private int attachedGamepadMask;
    private int encryptionFlags;

    private int httpPort;
    private int httpsPort;
    private int audioPort;
    private int videoPort;
    private int controlPort;
    private int rtspPort;
    private int pinPort;

    public static class Builder {
        private StreamConfiguration config = new StreamConfiguration();
        
        public StreamConfiguration.Builder setApp(NvApp app) {
            config.app = app;
            return this;
        }
        
        public StreamConfiguration.Builder setRemoteConfiguration(int remote) {
            config.remote = remote;
            return this;
        }
        
        public StreamConfiguration.Builder setResolution(int width, int height) {
            config.width = width;
            config.height = height;
            return this;
        }
        
        public StreamConfiguration.Builder setRefreshRate(int refreshRate) {
            config.refreshRate = refreshRate;
            return this;
        }

        public StreamConfiguration.Builder setLaunchRefreshRate(int refreshRate) {
            config.launchRefreshRate = refreshRate;
            return this;
        }
        
        public StreamConfiguration.Builder setBitrate(int bitrate) {
            config.bitrate = bitrate;
            return this;
        }
        
        public StreamConfiguration.Builder setEnableSops(boolean enable) {
            config.sops = enable;
            return this;
        }
        
        public StreamConfiguration.Builder enableAdaptiveResolution(boolean enable) {
            config.enableAdaptiveResolution = enable;
            return this;
        }
        
        public StreamConfiguration.Builder enableLocalAudioPlayback(boolean enable) {
            config.playLocalAudio = enable;
            return this;
        }
        
        public StreamConfiguration.Builder setMaxPacketSize(int maxPacketSize) {
            config.maxPacketSize = maxPacketSize;
            return this;
        }

        public StreamConfiguration.Builder setHevcBitratePercentageMultiplier(int multiplier) {
            config.hevcBitratePercentageMultiplier = multiplier;
            return this;
        }

        public StreamConfiguration.Builder setEnableHdr(boolean enableHdr) {
            config.enableHdr = enableHdr;
            return this;
        }

        public StreamConfiguration.Builder setAttachedGamepadMask(int attachedGamepadMask) {
            config.attachedGamepadMask = attachedGamepadMask;
            return this;
        }

        public StreamConfiguration.Builder setAttachedGamepadMaskByCount(int gamepadCount) {
            config.attachedGamepadMask = 0;
            for (int i = 0; i < 4; i++) {
                if (gamepadCount > i) {
                    config.attachedGamepadMask |= 1 << i;
                }
            }
            return this;
        }

        public StreamConfiguration.Builder setClientRefreshRateX100(int refreshRateX100) {
            config.clientRefreshRateX100 = refreshRateX100;
            return this;
        }

        public StreamConfiguration.Builder setAudioEncryption(boolean enable) {
            if (enable) {
                config.encryptionFlags |= MoonBridge.ENCFLG_AUDIO;
            }
            else {
                config.encryptionFlags &= ~MoonBridge.ENCFLG_AUDIO;
            }
            return this;
        }

        public StreamConfiguration.Builder setAudioConfiguration(MoonBridge.AudioConfiguration audioConfig) {
            config.audioConfiguration = audioConfig;
            return this;
        }
        
        public StreamConfiguration.Builder setHevcSupported(boolean supportsHevc) {
            config.supportsHevc = supportsHevc;
            return this;
        }

        public StreamConfiguration.Builder setHttpPort(int httpPort) {
            config.httpPort = httpPort;
            return this;
        }

        public StreamConfiguration.Builder setHttpsPort(int httpsPort) {
            config.httpsPort = httpsPort;
            return this;
        }

        public StreamConfiguration.Builder setAudioPort(int audioPort) {
            config.audioPort = audioPort;
            return this;
        }

        public StreamConfiguration.Builder setVideoPort(int videoPort) {
            config.videoPort = videoPort;
            return this;
        }

        public StreamConfiguration.Builder setControlPort(int controlPort) {
            config.controlPort = controlPort;
            return this;
        }

        public StreamConfiguration.Builder setRtspPort(int rtspPort) {
            config.rtspPort = rtspPort;
            return this;
        }
        
        public StreamConfiguration build() {
            return config;
        }
    }
    
    private StreamConfiguration() {
        // Set default attributes
        this.app = new NvApp("Steam");
        this.width = 1280;
        this.height = 720;
        this.refreshRate = 60;
        this.launchRefreshRate = 60;
        this.bitrate = 10000;
        this.maxPacketSize = 1024;
        this.remote = STREAM_CFG_AUTO;
        this.sops = true;
        this.enableAdaptiveResolution = false;
        this.audioConfiguration = MoonBridge.AUDIO_CONFIGURATION_STEREO;
        this.supportsHevc = false;
        this.enableHdr = false;
        this.attachedGamepadMask = 0;
        this.httpPort = MoonBridge.DEFAULT_HTTP_PORT;
        this.httpsPort = MoonBridge.DEFAULT_HTTPS_PORT;
        this.audioPort = MoonBridge.DEFAULT_AUDIO_PORT;
        this.videoPort = MoonBridge.DEFAULT_VIDEO_PORT;
        this.controlPort = MoonBridge.DEFAULT_CONTROL_PORT;
        this.rtspPort = MoonBridge.DEFAULT_RTSP_PORT;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getRefreshRate() {
        return refreshRate;
    }

    public int getLaunchRefreshRate() {
        return launchRefreshRate;
    }
    
    public int getBitrate() {
        return bitrate;
    }
    
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    public NvApp getApp() {
        return app;
    }
    
    public boolean getSops() {
        return sops;
    }
    
    public boolean getAdaptiveResolutionEnabled() {
        return enableAdaptiveResolution;
    }
    
    public boolean getPlayLocalAudio() {
        return playLocalAudio;
    }
    
    public int getRemote() {
        return remote;
    }

    public MoonBridge.AudioConfiguration getAudioConfiguration() {
        return audioConfiguration;
    }
    
    public boolean getHevcSupported() {
        return supportsHevc;
    }

    public int getHevcBitratePercentageMultiplier() {
        return hevcBitratePercentageMultiplier;
    }

    public boolean getEnableHdr() {
        return enableHdr;
    }

    public int getAttachedGamepadMask() {
        return attachedGamepadMask;
    }

    public int getClientRefreshRateX100() {
        return clientRefreshRateX100;
    }

    public int getEncryptionFlags() {
        return encryptionFlags;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public int getAudioPort() {
        return audioPort;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public int getControlPort() {
        return controlPort;
    }

    public int getRtspPort() {
        return rtspPort;
    }
}
