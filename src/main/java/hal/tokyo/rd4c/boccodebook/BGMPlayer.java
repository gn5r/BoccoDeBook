/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 *
 * @author gn5r
 */
public class BGMPlayer extends Thread {

    private final AudioInputStream ais;
    private final DataLine.Info dataLine;
    private final SourceDataLine cardBGM;
    private final byte[] data;

    private boolean flag;

    public BGMPlayer(int BGMNum) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        this.ais = AudioSystem.getAudioInputStream(new File(BGMDir(BGMNum)));

        AudioFormat format = ais.getFormat();
        this.dataLine = new DataLine.Info(SourceDataLine.class, format);

        this.cardBGM = (SourceDataLine) AudioSystem.getLine(this.dataLine);
        this.cardBGM.open();
        this.cardBGM.start();

        this.data = new byte[this.cardBGM.getBufferSize()];

        this.flag = true;
    }

    @Override
    public void run() {
        int size = -1;
        while (true) {
           
            try {
                
                size = this.ais.read(this.data);
                if (size == -1 || !flag) break;

                this.cardBGM.write(this.data, 0, size);
            } catch (Exception e) {
            }

        }
    }

    public void stopBGM() {
        this.flag = false;

        this.cardBGM.drain();
        this.cardBGM.stop();
        this.cardBGM.close();
    }

    private String BGMDir(int BGMNum) {
        String fileDir = "BGM/";

        switch (BGMNum) {
            /*    startカード    */
            case 0:
            case 1:
            case 2:
                fileDir += BGMNum + ".wav";
                break;

            /*    eventカード    */
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                fileDir += BGMNum + ".wav";
                break;

            /*    endカード    */
            case 12:
            case 13:
            case 14:
                fileDir += BGMNum + ".wav";
                break;
        }

        return fileDir;
    }
}
