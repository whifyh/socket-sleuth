/*
 * © 2023 Snyk Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package socketsleuth.intruder.payloads.models;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NumericPayloadModel implements IPayloadModel<String> {

    private int from;
    private int to;
    private int step;
    private int minDigits;
    private boolean isHexMode = false;

    public boolean isHexMode() {
        return isHexMode;
    }

    public void setHexMode(boolean hexMode) {
        isHexMode = hexMode;
    }

    public NumericPayloadModel(int from, int to, int step, int minDigits) {
        this.from = from;
        this.to = to;
        this.step = step;
        this.minDigits = minDigits;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void setMinDigits(int minDigits) {
        this.minDigits = minDigits;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<>() {
            int current = from;

            @Override
            public boolean hasNext() {
                return current <= to;
            }

            @Override
            public String next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                String result = formatNumber(current);
                current += step;
                return result;
            }

            private String formatNumber(int number) {
                if (isHexMode) {
                    // 返回十六进制格式的字符串
                    String hexFormat = "%" + minDigits + "s";
                    return String.format(hexFormat, Integer.toHexString(number)).replace(' ', '0').toUpperCase();
                } else {
                    // 返回十进制格式的字符串
                    String decimalFormat = "%0" + minDigits + "d";
                    return String.format(decimalFormat, number);
                }
            }
        };
    }
}