function [cfo,to] = synchronization(SF,up_index,down_index)
%SYNCHRONIZATION Summary of this function goes here
    if SF == 7
        compensate = 64;
    elseif SF == 6
        compensate = 32;
    elseif SF == 5
        compensate = 16;
    elseif SF == 4
        compensate = 8;
    end

    if up_index > compensate
        up_index = up_index - pow2(SF);
    end

    if down_index > compensate
        down_index = down_index - pow2(SF);
    end

    cfo = (up_index + down_index) / 2;
    to = (down_index - up_index) / 2;

end

