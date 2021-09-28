package com.vaca.fuckh264;

import android.media.MediaCodec;
import android.util.Log;

import java.nio.ByteBuffer;

public class FuckH265 {
    String TAG="fuck";

    //查找sps pps vps
    public void searchSPSandPPSFromH264(ByteBuffer buffer, MediaCodec.BufferInfo bufferInfo){

        byte[] csd = new byte[128];
        int len = 0, p = 4, q = 4;

        len = bufferInfo.size;
        Log.d(TAG,"len="+len);
        if (len<128) {
            buffer.get(csd,0,len);
            if (len>0 && csd[0]==0 && csd[1]==0 && csd[2]==0 && csd[3]==1) {
                // Parses the SPS and PPS, they could be in two different packets and in a different order
                //depending on the phone so we don't make any assumption about that
                while (p<len) {
                    while (!(csd[p+0]==0 && csd[p+1]==0 && csd[p+2]==0 && csd[p+3]==1) && p+3<len) p++;
                    if (p+3>=len) p=len;
                    if ((csd[q]&0x1F)==7) {
                        byte[] sps = new byte[p-q];
                        System.arraycopy(csd, q, sps, 0, p-q);
                        Log.d(TAG,"chris, searchSPSandPPSFromH264 SPS="+bytesToHex(sps));
                        //chris, searchSPSandPPSFromH264 SPS=6764001FACB402802DD2905020206D0A1350
                    } else {
                        byte[] pps = new byte[p-q];
                        System.arraycopy(csd, q, pps, 0, p-q);
                        Log.d(TAG,"chris, searchSPSandPPSFromH264 PPS="+bytesToHex(pps));
                        //chris, searchSPSandPPSFromH264 PPS=68EE06E2C0
                    }
                    p += 4;
                    q = p;
                }
            }
        }
    }

    public void searchVpsSpsPpsFromH265(ByteBuffer csd0byteBuffer) {
        int vpsPosition = -1;
        int spsPosition = -1;
        int ppsPosition = -1;
        int contBufferInitiation = 0;
        byte[] csdArray = csd0byteBuffer.array();
        for (int i = 0; i < csdArray.length; i++) {
            if (contBufferInitiation == 3 && csdArray[i] == 1) {
                if (vpsPosition == -1) {
                    vpsPosition = i - 3;
                } else if (spsPosition == -1) {
                    spsPosition = i - 3;
                } else {
                    ppsPosition = i - 3;
                }
            }
            if (csdArray[i] == 0) {
                contBufferInitiation++;
            } else {
                contBufferInitiation = 0;
            }
        }
        byte[] vps = new byte[spsPosition];
        byte[] sps = new byte[ppsPosition - spsPosition];
        byte[] pps = new byte[csdArray.length - ppsPosition];
        for (int i = 0; i < csdArray.length; i++) {
            if (i < spsPosition) {
                vps[i] = csdArray[i];
            } else if (i < ppsPosition) {
                sps[i - spsPosition] = csdArray[i];
            } else {
                pps[i - ppsPosition] = csdArray[i];
            }
        }


        Log.d(TAG, "searchVpsSpsPpsFromH265: vps="+ bytesToHex(vps)+",sps="+bytesToHex(sps)+",pps="+bytesToHex(pps));
        //vps=0000000140010C01FFFF016000000300B0000003000003005DAC59,sps=00000001420101016000000300B0000003000003005DA00280802E1F1396BB9324BB948281010176850940,pps=000000014401C0F1800420
    }


    public static String bytesToHex(byte[] src){
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
