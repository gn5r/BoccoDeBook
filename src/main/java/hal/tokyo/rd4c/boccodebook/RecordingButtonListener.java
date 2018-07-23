/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import hal.tokyo.rd4c.speech2text.GoogleSpeechAPI;
import hal.tokyo.rd4c.speech2text.MicroPhone;
import java.io.File;
import javax.sound.sampled.LineUnavailableException;

/**
 *
 * @author gn5r
 */
public class RecordingButtonListener implements GpioPinListenerDigital {

    private final BGMPlayer bgmPlayer;
    private final MicroPhone microPhone;
    private final GoogleSpeechAPI googleAPI;
    private int pushCnt;
    
    public RecordingButtonListener(BGMPlayer bgmPlayer,String GoogleAPIKey) {
        
        this.pushCnt = 0;
        this.bgmPlayer = bgmPlayer;
        this.microPhone = new MicroPhone();
        this.googleAPI = new GoogleSpeechAPI(GoogleAPIKey);
        
        try {
            this.microPhone.init();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        if (gpdsce.getState() == PinState.LOW) {
            
            this.pushCnt++;
            
            try {
                this.microPhone.startRec();
                this.microPhone.stopRec();
                
                File recData = this.microPhone.convertWav("voice");
                this.googleAPI.setFilePATH(recData.getPath());
                
                String result = this.googleAPI.postGoogleAPI();
                
                /*    音声認識が正常にされた場合    */
                if(!result.matches(null)){
                    
                }
                
                /*    録音ボタンが初めて押されたかどうかの検出    */
                if(this.pushCnt == 1){
                    /*    再生ボタンとステップボタンのリスナーをセット    */
                }
                
            } catch (Exception e) {
            }
            
            
        }
    }

}
