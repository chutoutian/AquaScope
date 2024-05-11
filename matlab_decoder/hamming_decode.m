function nibbles = hamming_decode(codewords, rdd)
            % hamming_decode  Hamming Decoding
            %
            % input:
            %     codewords: Codewords after deinterleaving
            %     rdd: Bits with redundancy
            % output:
            %     nibbles: Nibbles after hamming decoding

            p1 = LoRaPHY.bit_reduce(@bitxor, codewords, [8 4 3 1]);
            p2 = LoRaPHY.bit_reduce(@bitxor, codewords, [7 4 2 1]);
            p3 = LoRaPHY.bit_reduce(@bitxor, codewords, [5 3 2 1]);
            p4 = LoRaPHY.bit_reduce(@bitxor, codewords, [5 4 3 2 1]);
            p5 = LoRaPHY.bit_reduce(@bitxor, codewords, [6 4 3 2]);
            function pf = parity_fix(p)
                switch p
                    case 3 % 011 wrong b3
                        pf = 4;
                    case 5 % 101 wrong b4
                        pf = 8;
                    case 6 % 110 wrong b1
                        pf = 1;
                    case 7 % 111 wrong b2
                        pf = 2;
                    otherwise
                        pf = 0;
                end
            end
            switch rdd
                % TODO report parity error
                case {5, 6}
                    nibbles = mod(codewords, 16);
                case {7, 8}
                    parity = p2*4+p3*2+p5;
                    pf = arrayfun(@parity_fix, parity);
                    codewords = bitxor(codewords, uint16(pf));
                    nibbles = mod(codewords, 16);
                otherwise
                    % THIS CASE SHOULD NOT HAPPEN
                    error('Invalid Code Rate!');
            end
        end