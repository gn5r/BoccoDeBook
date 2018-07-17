/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import hal.tokyo.rd4c.bocco4j.BoccoAPI;

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

    public StepButtonListener(int stage, String mode, BoccoAPI boccoAPI, Main main) {
        this.stage = stage;
        this.mode = mode;
        this.boccoAPI = boccoAPI;
        this.main = main;
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        
        switch (this.mode) {
            case "cardSet":
                break;
            case "cardJudge":
                break;
            case "RecVoice":
                break;
        }
    }

}
