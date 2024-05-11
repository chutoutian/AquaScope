function c1 = chirp2(is_up, T, bw, fs)
            % chirp  Generate a LoRa chirp symbol
            %
            % input:
            %     is_up: `true` if constructing an up-chirp
            %            `false` if constructing a down-chirp
            %     sf: Spreading Factor
            %     bw: Bandwidth
            %     fs: Sampling Frequency
            %     h: Start frequency offset (0 to 2^SF-1)
            %     cfo: Carrier Frequency Offset
            %     tdelta: Time offset (0 to 1/fs)
            %     tscale: Scaling the sampling frequency
            % output:
            %     y: Generated LoRa symbol

            samp_per_sym = T * fs;
            if is_up
                k = bw/T;
                f0 = -bw/2;
            else
                k = -bw/T;
                f0 = bw/2;
            end

            % retain last element to calculate phase
            t = (0:samp_per_sym-1)/fs;
            c1 = exp(1j*2*pi*(t.*(f0+0.5*k*t)));

        end