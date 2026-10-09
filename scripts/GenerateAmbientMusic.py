"""Reproduce the original Space Compass meditative score. Requires NumPy and imageio-ffmpeg.

Copyright (c) 2026 Mondiversi. GPL-3.0. No external recordings or samples.
The integer-period oscillators, circular envelopes and reverberation form a seamless loop.
"""
from pathlib import Path
import argparse, hashlib, json, subprocess, wave
import numpy as np

RATE, SECONDS = 44100, 96

def render(output: Path, ffmpeg: str | None = None):
    if ffmpeg is None:
        import imageio_ffmpeg
        ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    count = RATE * SECONDS
    t = np.arange(count, dtype=np.float64) / RATE
    mix = np.zeros((count, 2), np.float32)
    rng = np.random.default_rng(426121)

    def frequency(note):
        return round(440 * 2 ** ((note - 69) / 12) * SECONDS) / SECONDS

    def voice(note, envelope, level, pan, phase=0.0):
        f = frequency(note)
        # Each detuned frequency and modulation completes an integer number of cycles.
        detuned = round(f * 1.0018 * SECONDS) / SECONDS
        sound = (np.sin(2*np.pi*f*t+phase) + .22*np.sin(2*np.pi*detuned*t+phase+.4)
                 + .07*np.sin(2*np.pi*2*f*t+phase))
        breath = .9 + .1*np.sin(2*np.pi*3*t/SECONDS+phase)
        signal = (sound * envelope * breath * level).astype(np.float32)
        mix[:,0] += signal*np.sqrt((1-pan)/2)
        mix[:,1] += signal*np.sqrt((1+pan)/2)

    # Slow overlapping major ninth / suspended colors; no beat or percussive accents.
    chords = [[38,50,57,61,64,66], [35,47,54,57,61,64],
              [31,43,50,54,57,59], [33,45,52,57,59,64]]
    for index, chord in enumerate(chords):
        center = 12 + index*24
        distance = (t-center+SECONDS/2) % SECONDS - SECONDS/2
        envelope = np.where(np.abs(distance)<24, np.cos(np.pi*distance/48)**2, 0).astype(np.float32)
        for j, note in enumerate(chord):
            voice(note,envelope,.022 if j==0 else .038, -.75+1.5*j/(len(chord)-1), float(rng.uniform(0,2*np.pi)))
    # Quiet continuous fifth underneath the changing pads.
    for note,pan in ((50,-.25),(57,.25)):
        voice(note,np.ones(count,np.float32),.014,pan)
    # Long, soft upper tones emerge gradually from the space, rather than sounding like bells.
    for index,note in enumerate([74,78,81,78,73,76,78,73,71,74,78,74]):
        center=8+index*8
        distance=(t-center+SECONDS/2)%SECONDS-SECONDS/2
        envelope=np.exp(-(distance/3.8)**2).astype(np.float32)
        voice(note,envelope,.010,-.65 if index%2==0 else .65,index*.31)

    dry = mix.copy()
    # Circular diffuse reflections preserve the seam; stereo reflections remain correlated enough for mono.
    for seconds,level in ((.19,.12),(.47,.10),(.83,.08),(1.37,.065),(2.11,.05),(3.29,.035)):
        mix += np.roll(dry[:,::-1],round(seconds*RATE),axis=0)*level
    mix *= .46 / float(np.max(np.abs(mix)))
    # Let codec start/end windows meet quietly; the half-second soft breath has no silence gap.
    edge = np.minimum(t, SECONDS-t)
    mix *= (np.sin(np.pi*.5*np.minimum(edge/.45,1))**2)[:,None]
    output.parent.mkdir(parents=True,exist_ok=True)
    wav=output.with_suffix('.wav')
    with wave.open(str(wav),'wb') as audio:
        audio.setnchannels(2); audio.setsampwidth(2); audio.setframerate(RATE)
        audio.writeframes((mix*32767).astype('<i2').tobytes())
    result=subprocess.run([ffmpeg,'-y','-hide_banner','-loglevel','error','-i',str(wav),
        '-c:a','libvorbis','-q:a','1','-map_metadata','-1','-metadata','title=Quiet Orbit',
        '-metadata','artist=Mondiversi','-metadata','copyright=Copyright 2026 Mondiversi; GPL-3.0',str(output)],capture_output=True)
    assert result.returncode==0,'Ogg encoding failed'
    # Decode the shipped file, including the transition back to the first sample.
    result=subprocess.run([ffmpeg,'-hide_banner','-loglevel','error','-i',str(output),
        '-f','f32le','-ac','2','-ar',str(RATE),'-'],capture_output=True)
    assert result.returncode==0
    decoded=np.frombuffer(result.stdout,dtype='<f4').reshape(-1,2)
    assert len(decoded)==count and np.isfinite(decoded).all()
    seam=np.max(np.abs(decoded[-1]-decoded[0]))
    peak=float(np.max(np.abs(decoded)))
    assert peak<.65 and seam<.001,(peak,seam)
    report={'title':'Quiet Orbit','artist':'Mondiversi','original_composition':True,
        'no_external_samples':True,'license':'GPL-3.0','duration_seconds':SECONDS,'sample_rate':RATE,
        'channels':2,'codec':'Vorbis','seamless_loop':True,'decoded_loop_seam_step':float(seam),
        'decoded_peak_dbfs':float(20*np.log10(peak)),'decoded_rms_dbfs':float(20*np.log10(np.sqrt(np.mean(decoded**2)))),
        'bytes':output.stat().st_size,'sha256':hashlib.sha256(output.read_bytes()).hexdigest()}
    output.with_suffix('.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report,indent=2))

if __name__=='__main__':
    parser=argparse.ArgumentParser(); parser.add_argument('output',type=Path)
    parser.add_argument('--ffmpeg', help='Use an existing FFmpeg executable instead of imageio-ffmpeg.')
    args=parser.parse_args()
    render(args.output, args.ffmpeg)
