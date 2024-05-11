function longs = Bytes2Embedding(bytes)
    % Step 1: Convert bytes to a binary string
    binaryString = '';
    for i = 1:length(bytes)
        % Ensure the byte is treated as unsigned
        if bytes(i) < 0
            bytes(i) = bytes(i) + 256;
        end
        % Convert byte to binary string of length 8
        byteBin = dec2bin(bytes(i), 8);
        % Concatenate to the full binary string
        binaryString = [binaryString byteBin];
    end

    % Step 2: Determine how many 10-bit numbers are needed
    numInts = floor(length(binaryString) / 10);

    % Step 3: Convert each 10-bit binary string to a decimal (long integer)
    longs = zeros(1, numInts);
    for i = 1:numInts
        % Extract the 10-bit segment
        intString = binaryString((i-1)*10 + 1:i*10);
        % Convert binary string to a number
        newLong = bin2dec(intString);
        % Store the number
        longs(i) = newLong;
    end

end

