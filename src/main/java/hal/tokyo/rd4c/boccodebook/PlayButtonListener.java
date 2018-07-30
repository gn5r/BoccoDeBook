/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hal.tokyo.rd4c.boccodebook;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

/**
 *
 * @author gn5r
 */
public class PlayButtonListener implements GpioPinListenerDigital {

    private final Main main;

    public PlayButtonListener() {
        this.main = new Main();
    }

    @Override
    public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent gpdsce) {
        if (gpdsce.getState() == PinState.LOW) {

            try {
                /* Boccoへ直近の変換文字列を送信 */
                this.main.recentlySend();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
