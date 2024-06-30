import os
import numpy as np
import matplotlib.pyplot as plt
from scipy.stats import cumfreq

preamble_tim = 195
fs = 48000
ChirpGap = 960
Ns = 960
Cp = 67
Gap =240
chanest_symreps = 8
valid_carrier_default = list(range(20, 80))
pn60_syms = [
    [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1],
    [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1],
    [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1],
    [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1],
    [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1],
    [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [-1, -1, -1, -1, -1], [1, 1, 1, 1, 1], [1, 1, 1, 1, 1]
]

def save_snrs_to_file(snrs, output_path):
    snrs_str = ",".join(map(str, snrs))
    with open(output_path, 'w') as file:
        file.write(snrs_str)


def calculate_snr(rx_spectrum, gt_symbol, sym_start, sym_end):
    bin_num = len(gt_symbol)
    snr_list = np.zeros(bin_num)
    sym_count = sym_end - sym_start

    for i in range(bin_num):
        H = [0, 0]
        for j in range(sym_start, sym_end):
            H[0] += rx_spectrum[0][i][j] * gt_symbol[i][0]
            H[1] += rx_spectrum[1][i][j] * gt_symbol[i][0]
        
        H[0] /= sym_count
        H[1] /= sym_count
        
        noise_level = 0
        for j in range(sym_start, sym_end):
            noise_level += (rx_spectrum[0][i][j] * gt_symbol[i][0] - H[0])**2 + (H[1] - rx_spectrum[1][i][j] * gt_symbol[i][0])**2
        
        noise_level /= sym_count
        signal_level = H[0]**2 + H[1]**2
        snr_list[i] = 10 * np.log10(signal_level / noise_level)
    
    return snr_list

def calculate_snr2(rx_spectrum, gt_symbol, sym_start, sym_end):
    bin_num = len(valid_carrier_default)
    snr_list = np.zeros(bin_num)
    sym_count = sym_end - sym_start

    for i in range(bin_num):
        H = [0, 0]
        for j in range(sym_start, sym_end):
            H[0] += rx_spectrum[0][i][j] * gt_symbol[0][i][j]
            H[1] += rx_spectrum[1][i][j] * gt_symbol[0][i][j]
        
        H[0] /= sym_count
        H[1] /= sym_count
        
        noise_level = 0
        for j in range(sym_start, sym_end):
            noise_level += (rx_spectrum[0][i][j] * gt_symbol[0][i][j] - H[0])**2 + (H[1] - rx_spectrum[1][i][j] * gt_symbol[0][i][j])**2
        
        noise_level /= sym_count
        signal_level = H[0]**2 + H[1]**2
        snr_list[i] = 10 * np.log10(signal_level / noise_level)
    
    return snr_list

def get_preamble(rec, start_point,gt_symbol):
    rec = np.array(rec)
    rx_preamble_start = start_point + 240
    rx_preamble_end = rx_preamble_start + int((preamble_tim / 1000.0) * fs) - 1 

    if rx_preamble_end - 1 > len(rec) or rx_preamble_start < 0:
        print(f"Error extracting preamble from sounding signal {rx_preamble_start},{rx_preamble_end}")
        return []

    rx_sym_start = rx_preamble_end + ChirpGap + 1 
    rx_sym_end = rx_sym_start + ((Ns + Cp) * chanest_symreps) - 1 

    if rx_sym_end - 1 > len(rec) or rx_sym_start < 0:
        print("Error extracting preamble from sounding signal")
        return []

    rx_symbols = rec[rx_sym_start:rx_sym_end+1]
    rx_symbols = rx_symbols / 30000

    freq_spacing = fs / Ns
    fseq = np.linspace(1000, freq_spacing, 4000)

    nbin1_default = round(1000 / freq_spacing)
    nbin2_default = round(4000 / freq_spacing) - 1
    subcarrier_number_default = nbin2_default - nbin1_default + 1;

    spec_est = np.zeros((2, subcarrier_number_default, chanest_symreps))
    cc = Cp

    for i in range(chanest_symreps):
        seg = rx_symbols[cc:cc + Ns]
        spec = np.fft.fft(seg)

        bin_counter = 0
        for bin in valid_carrier_default:
            real_part = np.real(spec[bin])
            imag_part = np.imag(spec[bin])
            spec_est[0, bin_counter, i] = real_part
            spec_est[1, bin_counter, i] = imag_part
            bin_counter += 1

        cc += Ns + Cp



    snrs = calculate_snr(spec_est, pn60_syms, 1, chanest_symreps)

    return snrs

def get_preamble2(rec, start_point,gt_symbols):
    rec = np.array(rec)
    gt_symbols = np.array(gt_symbols)
    rx_preamble_start = start_point
    rx_preamble_end = rx_preamble_start + int((preamble_tim / 1000.0) * fs) - 1 

    if rx_preamble_end - 1 > len(rec) or rx_preamble_start < 0:
        print(f"Error extracting preamble from sounding signal {rx_preamble_start},{rx_preamble_end}")
        return []

    #rx_sym_start = rx_preamble_end + ChirpGap + 1 
    #rx_sym_end = rx_sym_start + ((Ns + Cp) * chanest_symreps) - 1 

    #if rx_sym_end - 1 > len(rec) or rx_sym_start < 0:
    #    print("Error extracting preamble from sounding signal")
    #    return []

    rx_symbols = rec[rx_preamble_start:rx_preamble_end+1]

    freq_spacing = fs / Ns

    nbin1_default = round(1000 / freq_spacing)
    nbin2_default = round(4000 / freq_spacing) - 1
    subcarrier_number_default = nbin2_default - nbin1_default + 1;

    spec_est = np.zeros((2, subcarrier_number_default, chanest_symreps))
    spec_gt = np.zeros((2,subcarrier_number_default,chanest_symreps))
    cc = 0

    for i in range(chanest_symreps):
        seg = rx_symbols[cc:cc + Ns]
        seg_gt = gt_symbols[cc:cc + Ns]
        spec = np.fft.fft(seg)
        spec_gt_tmp = np.fft.fft(seg_gt)

        bin_counter = 0
        for bin in valid_carrier_default:
            real_part = np.real(spec[bin])
            imag_part = np.imag(spec[bin])
            real_part_gt = np.real(spec_gt_tmp[bin])
            imag_part_gt = np.imag(spec_gt_tmp[bin])
            spec_est[0, bin_counter, i] = real_part
            spec_est[1, bin_counter, i] = imag_part
            spec_gt[0, bin_counter, i] = real_part_gt
            spec_gt[1, bin_counter, i] = imag_part_gt
            
            bin_counter += 1

        cc += Ns + Gap



    snrs = calculate_snr2(spec_est, spec_gt, 1, chanest_symreps)

    return snrs


def embedding_to_bits_bytes(embedding):
    binary_string_builder = []

    # Step 1: Convert all integers to a single binary string with each being a 10-bit segment
    for value in embedding:
        # Mask with 0x3FF to ensure only the lowest 10 bits are used
        binary_string = format(value & 0x3FF, '010b')
        binary_string_builder.append(binary_string)

    # Join all binary segments into one string
    all_bits = ''.join(binary_string_builder)
    num_bytes = (len(all_bits) + 7) // 8  # Calculate how many bytes are needed
    byte_list = bytearray(num_bytes)

    # Step 2: Convert the binary string into a byte array
    for i in range(num_bytes):
        byte_string = all_bits[i*8:(i+1)*8].ljust(8, '0')  # Pad to 8 bits if necessary
        byte_list[i] = int(byte_string, 2)

    return all_bits,byte_list

def read_embedding(file_path):
    with open(file_path, 'r') as file:
        data = file.read().strip()
        # Remove the square brackets and split by comma
        data = data[1:-1].split(',')
        embedding = [int(value.strip()) for value in data]
    return np.array(embedding)

def read_snr(file_path):
    with open(file_path, 'r') as file:
        line = file.readline()
        snr_values = [float(value) for value in line.strip().split(',')]
    return snr_values

def read_symbol(file_path):
    with open(file_path, 'r') as file:
        data = [float(line.strip()) for line in file]
    return data

def calculate_embedding_error_rate(ground_truth, recovered):
    errors = np.sum(ground_truth != recovered)
    return errors / len(ground_truth)

def read_sensor_data(file_path):
    with open(file_path, 'r') as file:
        data = file.readlines()
    return [list(map(float, line.split(','))) for line in data]

def calculate_byte_error_rate(ground_truth_bytes, recovered_bytes):
    ground_truth_array = np.array(ground_truth_bytes)
    recovered_array = np.array(recovered_bytes)
    # Calculate the number of errors
    errors = np.sum(ground_truth_array != recovered_array)
    
    # Calculate and return the byte error rate
    return errors / len(ground_truth_array)

def calculate_bit_error_rate(ground_truth_bits, recovered_bits):
    ground_truth_bits_array = np.array([int(bit) for bit in ground_truth_bits])
    recovered_bits_array = np.array([int(bit) for bit in recovered_bits])
    
    # Calculate the number of errors
    errors = np.sum(ground_truth_bits_array != recovered_bits_array)
    
    # Calculate and return the bit error rate
    return errors / len(ground_truth_bits_array)

def get_files(location, distance, motion_pattern, depth,direction, sensory, base_folder='sampledataset_0618/image1'):
    schemes = ['css', 'ofdm_adapt', 'ofdm_wo_adapt', 'proposed']
    folder_name = f"{location}_{distance}_{motion_pattern}_{depth}_{direction}"
    files = []

    for scheme in schemes:
        scheme_folder = os.path.join(base_folder, scheme, folder_name)
        if not os.path.isdir(scheme_folder):
            continue
        for sample_folder in os.listdir(scheme_folder):
            if sample_folder == '.DS_Store':
                continue
            if sensory == "acc":
                sample_path = os.path.join(scheme_folder, sample_folder, 'acc-Bob-Receiver_Sensor-0.txt')
            elif sensory == "gyro":
                sample_path = os.path.join(scheme_folder, sample_folder, 'gyro-Bob-Receiver_Sensor-0.txt')
            else:
                sample_path = os.path.join(scheme_folder, sample_folder, 'Bob-Rx_Embedding_Recovered-0.txt')
            if os.path.isfile(sample_path):
                files.append((scheme, sample_path))
    return files

def plot_cdf(data_dict, metric, location, motion_pattern,distance,depth,direction):
    plt.figure(figsize=(10, 6))

    for scheme, data in data_dict.items():
        res = cumfreq(data, numbins=100)
        x = res.lowerlimit + np.linspace(0, res.binsize * res.cumcount.size, res.cumcount.size)
        plt.step(x, res.cumcount / len(data), where='mid', label=scheme)

    #plt.title(title)
    plt.xlabel(f"{metric}")
    plt.ylabel('CDF')
    plt.legend()
    plt.grid(True)
    plt.title(f'CDF of {metric} at {location}_{distance}_{motion_pattern}_{depth}_{direction}')
    plt.savefig(f"underwater_figures/{metric}_{location}_{distance}_{motion_pattern}_{depth}_{direction}.png")
    plt.show()

def plot_cdf_sensor(data_dict, metric, location, motion_pattern, distance, depth, direction):
    if metric == "acc":
        directions = ['X', 'Y', 'Z']
    elif metric == "gyro":
        directions = ['S', 'T', 'V']
    
    for i, direction_label in enumerate(directions):
        plt.figure(figsize=(10, 6))

        for scheme, data in data_dict.items():
            # Extract the i-th element from each data point for the specific direction
            direction_data = [d[i] for d in data]
            mean_value = np.mean(direction_data)
            res = cumfreq(direction_data, numbins=100)
            x = res.lowerlimit + np.linspace(0, res.binsize * res.cumcount.size, res.cumcount.size)
            plt.step(x, res.cumcount / len(direction_data), where='mid', label=f"{scheme} (Mean: {mean_value:.2f})")

        plt.xlabel(f"{metric} ({direction_label}-axis)")
        plt.ylabel('CDF')
        plt.legend()
        plt.grid(True)
        plt.title(f'CDF of {metric} ({direction_label}-axis) at {location}_{distance}_{motion_pattern}_{depth}_{direction}')
        plt.savefig(f"underwater_figures/{metric}_{location}_{distance}_{motion_pattern}_{depth}_{direction}_{direction_label}.png")
        plt.show()

def plot_bar(data, setup,other_params, metric):
    x  = np.arange(len(data['x_labels']))  # the label locations
    width = 0.2  # the width of the bars

    fig, ax = plt.subplots(figsize=(12, 8))
    for i, scheme in enumerate(data['schemes']):
        ax.bar(x + i * width, data['error_rates'][scheme], width, label=scheme) ## 

    ax.set_xlabel(setup.capitalize())
    ax.set_ylabel(f'Average {metric}')
    ax.set_title(f'Average {metric} vs {setup.capitalize()}')
    ax.set_xticks(x + width * (len(data['schemes']) / 2 - 0.5))
    ax.set_xticklabels(data['x_labels'])
    ax.legend()

    plt.grid(True)
    plt.savefig(f"underwater_figures/bar_{metric}_by_{setup}.png")
    plt.show()

def plot_line(data, setup, other_params, metric):
    x = np.arange(len(data['x_labels']))  # the label locations

    fig, ax = plt.subplots(figsize=(12, 8))
    for scheme in data['schemes']:
        ax.plot(x, data['error_rates'][scheme], marker='o', label=scheme)

    # Remove the focus_param from other_params
    other_params_for_title = {k: v for k, v in other_params.items() if k != setup}
    other_params_str = "_".join([str(v) for v in other_params_for_title.values()])

    ax.set_xlabel(setup.capitalize())
    ax.set_ylabel(f'Average {metric}')
    ax.set_title(f'Average {metric} vs {setup.capitalize()} at {other_params_str}')
    ax.set_xticks(x)
    ax.set_xticklabels(data['x_labels'])
    ax.legend()

    plt.grid(True)
    plt.savefig(f"underwater_figures/line_{metric}_by_{setup}.png")
    plt.show()


def plot_snr(snr_values, location, motion_pattern, distance, depth, direction):
    frequencies = np.linspace(1000, 4000, len(snr_values)) 
    
    plt.figure()
    plt.plot(frequencies, snr_values, marker='o')
    plt.xlabel('Frequency (Hz)')
    plt.ylabel('SNR (dB)')
    plt.grid(True)
    plt.savefig(f"underwater_figures/snr_{location}_{distance}_{motion_pattern}_{depth}_{direction}.png")
    plt.show()

def generate_cdf(location, distance, motion_pattern, depth, direction, metric, base_folder):
    image_folders = [folder for folder in os.listdir(base_folder) if os.path.isdir(os.path.join(base_folder, folder))]
    results = {}

    
    if metric in ['Byte_Error_Rate', 'Bit_Error_Rate', 'Embedding_Error_Rate']:
        for image_folder in image_folders:
            ground_truth_path = os.path.join(base_folder, image_folder, 'groundtruth', 'Alice-Send_Embedding_Sequence-0.txt')
            ground_truth_embedding = read_embedding(ground_truth_path)
            ground_truth_bits, ground_truth_bytes = embedding_to_bits_bytes(ground_truth_embedding)
            
            files = get_files(location, distance, motion_pattern, depth, direction, metric, os.path.join(base_folder, image_folder))

            for scheme, file_path in files:
                recovered_embedding = read_embedding(file_path)
                recovered_bits, recovered_bytes = embedding_to_bits_bytes(recovered_embedding)
                
                if metric == 'Byte_Error_Rate':
                    error_rate = calculate_byte_error_rate(ground_truth_bytes, recovered_bytes)
                elif metric == 'Bit_Error_Rate':
                    error_rate = calculate_bit_error_rate(ground_truth_bits, recovered_bits)
                elif metric == 'Embedding_Error_Rate':
                    error_rate = calculate_embedding_error_rate(ground_truth_embedding, recovered_embedding)
                else:
                    raise ValueError("Invalid metric specified. Use 'BER' for Bit Error Rate or 'ByER' for Byte Error Rate.")

                if scheme not in results:
                    results[scheme] = []
                results[scheme].append(error_rate)
        plot_cdf(results, metric, location, motion_pattern, distance, depth, direction)
    elif metric in ['acc','gyro']:
        for image_folder in image_folders:
            files = get_files(location, distance, motion_pattern, depth, direction, metric, os.path.join(base_folder, image_folder))
            for scheme, file_path in files:
                acc_data = read_sensor_data(file_path)
                if scheme not in results:
                    results[scheme] = []
                results[scheme].append(acc_data)
        plot_cdf_sensor(results, metric, location, motion_pattern, distance, depth, direction)

    

def generate_bar_line(focus_param, other_params, metric, base_folder, fig_type):

    schemes = ['css', 'ofdm_adapt', 'ofdm_wo_adapt', 'proposed']

    setups = {
        'location': ['pool'],
        'distance': [1],
        'motion_pattern': ['static'],
        'depth': [1],
        'direction': [0]
    }  


    focus_values = setups.get(focus_param, [])
    
    data = {'x_labels': focus_values, 'schemes': schemes, 'error_rates': {scheme: [] for scheme in schemes}}


    image_folders = [folder for folder in os.listdir(base_folder) if os.path.isdir(os.path.join(base_folder, folder))]

    for image_folder in image_folders:
        ground_truth_path = os.path.join(base_folder, image_folder, 'groundtruth', 'Alice-Send_Embedding_Sequence-0.txt')
        ground_truth_embedding = read_embedding(ground_truth_path)
        ground_truth_bits, ground_truth_bytes = embedding_to_bits_bytes(ground_truth_embedding)
        
        for s in focus_values:
            kwargs = other_params.copy()
            kwargs[focus_param] = s
            for scheme in schemes:
                folder_name = f"{kwargs['location']}_{kwargs['distance']}m_{kwargs['motion_pattern']}_{kwargs['depth']}m_{kwargs['direction']}"
                scheme_folder = os.path.join(base_folder, image_folder, scheme, folder_name)
                if not os.path.isdir(scheme_folder):
                    continue
                error_rates = []
                for sample_folder in os.listdir(scheme_folder):
                    if sample_folder == '.DS_Store':
                        continue
                    sample_path = os.path.join(scheme_folder, sample_folder, 'Bob-Rx_Embedding_Recovered-0.txt')
                    if os.path.isfile(sample_path):
                        recovered_embedding = read_embedding(sample_path)
                        recovered_bits, recovered_bytes = embedding_to_bits_bytes(recovered_embedding)
                        if metric == 'Byte_Error_Rate':
                            error_rate = calculate_byte_error_rate(ground_truth_bytes, recovered_bytes)
                        elif metric == 'Bit_Error_Rate':
                            error_rate = calculate_bit_error_rate(ground_truth_bits, recovered_bits)
                        elif metric == 'Embedding_Error_Rate':
                            error_rate = calculate_embedding_error_rate(ground_truth_embedding, recovered_embedding)
                        else:
                            raise ValueError("Invalid metric specified. Use 'BER' for Bit Error Rate or 'ByER' for Byte Error Rate.")
                        error_rates.append(error_rate)
                if error_rates:
                    data['error_rates'][scheme].append(np.mean(error_rates))
                else:
                    data['error_rates'][scheme].append(0)  # Handle cases with no data

    if fig_type == 'line':
        plot_line(data, focus_param, other_params, metric)
    elif fig_type == 'bar':
        plot_bar(data,focus_param, other_params, metric)

def generate_snr(location, distance, motion_pattern, depth, direction, base_folder):
    image_folders = [folder for folder in os.listdir(base_folder) if os.path.isdir(os.path.join(base_folder, folder))]
    all_snr_values = []

    for image_folder in image_folders:
        proposed_folder = os.path.join(base_folder, image_folder, 'proposed')
        folder_name = f"{location}_{distance}_{motion_pattern}_{depth}_{direction}"
        setup_folder = os.path.join(proposed_folder, folder_name)
        
        if os.path.isdir(setup_folder):
            sample_snr_values = []
            for sample in os.listdir(setup_folder):
                if sample == '.DS_Store':
                    continue
                sample_path = os.path.join(setup_folder, sample, 'Bob-SNRs-0.txt')
                if os.path.isfile(sample_path):
                    sample_snr_values.append(read_snr(sample_path))
            
            if sample_snr_values:
                # Average SNR values across all samples in this setup
                sample_snr_values = np.mean(np.array(sample_snr_values), axis=0)
                all_snr_values.append(sample_snr_values)

    if not all_snr_values:
        print("No SNR values found for the given setup.")
        return

    # Average the SNR values across all images
    snr_values = np.mean(np.array(all_snr_values), axis=0)
    

    plot_snr(snr_values, location, motion_pattern, distance, depth, direction)

def generate_snr2(location, distance, motion_pattern, depth, direction, base_folder):
    image_folders = [folder for folder in os.listdir(base_folder) if os.path.isdir(os.path.join(base_folder, folder))]
    gt_symbol_path = os.path.join(base_folder, 'naiser3.txt')
    gt_symbol = read_symbol(gt_symbol_path)

    for image_folder in image_folders:
        proposed_folder = os.path.join(base_folder, image_folder, 'proposed')
        folder_name = f"{location}_{distance}_{motion_pattern}_{depth}_{direction}"
        setup_folder = os.path.join(proposed_folder, folder_name)
        
        if os.path.isdir(setup_folder):
            for sample in os.listdir(setup_folder):
                if sample == '.DS_Store':
                    continue
                sample_path = os.path.join(setup_folder, sample, 'Bob-Before_Equalization_Rx_Raw_Symbols-0.txt')
                if os.path.isfile(sample_path):
                    sample_snr_values = read_symbol(sample_path)
                    snrs = get_preamble2(sample_snr_values,0,gt_symbol)
                    save_snrs_to_file(snrs,os.path.join(setup_folder, sample,'Bob-SNRs-0.txt'))
                    

    

            

    

if __name__ == '__main__':
    
    
    default_setup = {
    'location': 'pool',
    'distance': 1,
    'motion_pattern': 'static',
    'depth': 1,
    'direction': 0
    }

    different_quanlity = ['Byte_Error_Rate', 'Bit_Error_Rate', 'Embedding_Error_Rate', 'acc','gyro']

    
    #generate_bar_line('distance', default_setup,'Byte_Error_Rate','06282024_arc_pool_experiment/06282024_arc_pool','line')
    
    #generate_cdf('pool', '1m', 'static','1m','0','acc','06282024_arc_pool_experiment/06282024_arc_pool')
    
    ##  calculate snr from the raw data and store it under the current dir
    #generate_snr2('pool', '1m', 'static', '1m', '0','06282024_arc_pool_experiment/06282024_arc_pool')
    
    ##  draw the snr figure
    generate_snr('pool', '1m', 'static', '1m', '0','06282024_arc_pool_experiment/06282024_arc_pool')

    


    