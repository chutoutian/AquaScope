function sym_num = calc_sym_num(plen,cr,sf)
%CALC_SYM_NUM Summary of this function goes here
%   Detailed explanation goes here
sym_num = double(8 + max((4+cr)*ceil(double((2*plen-sf+7-5))/double(sf)), 0));
end

