/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.mynewmedia4.exoplayer.rtsp;

import static androidx.mynewmedia4.common.util.Assertions.checkArgument;
import static androidx.mynewmedia4.common.util.Assertions.checkState;

import android.net.Uri;
import androidx.annotation.Nullable;
import androidx.mynewmedia4.common.C;
import androidx.mynewmedia4.common.PlaybackException;
import androidx.mynewmedia4.common.util.UnstableApi;
import androidx.mynewmedia4.common.util.Util;
import androidx.mynewmedia4.datasource.DataSpec;
import androidx.mynewmedia4.datasource.TransferListener;
import androidx.mynewmedia4.datasource.UdpDataSource;
import com.google.common.primitives.Ints;
import java.io.IOException;

/** An {@link RtpDataChannel} for UDP transport. */
@UnstableApi
/* package */ final class UdpDataSourceRtpDataChannel implements RtpDataChannel {

  private static final String DEFAULT_UDP_TRANSPORT_FORMAT = "RTP/AVP;unicast;client_port=%d-%d";

  private final UdpDataSource dataSource;

  /** The associated RTCP channel; {@code null} if the current channel is an RTCP channel. */
  @Nullable private UdpDataSourceRtpDataChannel rtcpChannel;

  /**
   * Creates a new instance.
   *
   * @param socketTimeoutMs The timeout for {@link #read} in milliseconds.
   */
  public UdpDataSourceRtpDataChannel(long socketTimeoutMs) {
    dataSource =
        new UdpDataSource(UdpDataSource.DEFAULT_MAX_PACKET_SIZE, Ints.checkedCast(socketTimeoutMs));
  }

  @Override
  public String getTransport() {
    int dataPortNumber = getLocalPort();
    checkState(dataPortNumber != C.INDEX_UNSET); // Assert open() is called.
    return Util.formatInvariant(DEFAULT_UDP_TRANSPORT_FORMAT, dataPortNumber, dataPortNumber + 1);
  }

  @Override
  public int getLocalPort() {
    int port = dataSource.getLocalPort();
    return port == UdpDataSource.UDP_PORT_UNSET ? C.INDEX_UNSET : port;
  }

  @Override
  public boolean needsClosingOnLoadCompletion() {
    return true;
  }

  @Nullable
  @Override
  public RtspMessageChannel.InterleavedBinaryDataListener getInterleavedBinaryDataListener() {
    return null;
  }

  @Override
  public void addTransferListener(TransferListener transferListener) {
    dataSource.addTransferListener(transferListener);
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    return dataSource.open(dataSpec);
  }

  @Nullable
  @Override
  public Uri getUri() {
    return dataSource.getUri();
  }

  @Override
  public void close() {
    dataSource.close();

    if (rtcpChannel != null) {
      rtcpChannel.close();
    }
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    try {
      return dataSource.read(buffer, offset, length);
    } catch (UdpDataSource.UdpDataSourceException e) {
      if (e.reason == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) {
        return C.RESULT_END_OF_INPUT;
      } else {
        throw e;
      }
    }
  }

  public void setRtcpChannel(UdpDataSourceRtpDataChannel rtcpChannel) {
    checkArgument(this != rtcpChannel);
    this.rtcpChannel = rtcpChannel;
  }
}
