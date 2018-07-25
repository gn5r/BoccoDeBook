/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import hal.tokyo.rd4c.bocco4j.BoccoAPI;
import hal.tokyo.rd4c.nfc.NFCReader;
import hal.tokyo.rd4c.nfc.RaspRC522;

/**
 *
 * @author gn5r
 */
public class StepButtonListener implements GpioPinListenerDigital {

    /*    カードの読取り枚数    */
    private final int stage;
    private final String mode;
    private final BoccoAPI boccoAPI;
    private final Main main;
    
    private RaspRC522 rsp;

    public StepButtonListener(int stage, String mode, BoccoAPI boccoAPI) {
        this.stage = stage;
        this.mode = mode;
        this.boccoAPI = boccoAPI;
        this.main = new Main();
        
        /* RC522_Initを複数呼び出すと危険なため */
        rsp = new RaspRC522();
        rsp.RC522_Init();
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {

        switch (this.mode) {
            case "cardSetEnd": {
                try {
                    /* カード認識＆カード判定に進む */
                    this.main.cardJudge(stage);
                    
                    /* 録音リスナーセット */
                    this.main.gamePlay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;

            /* 終わりのカードか判断する。終わりならwhileから抜ける */
            case "recVoiceEnd":
                this.main.BGMStop();
                this.main.endJudge();
                break;
        }
    }
}
