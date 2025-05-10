# PYTVDroid

Fork of [PyTorch Video Classification on Android](https://github.com/pytorch/android-demo-app/tree/master/TorchVideo) to make it use for https://github.com/JessieChenHui/titration  


## Quick Start

### 1. Prepare the Model (Optional)

#### Requirements

- Python < 3.10
- torch < 1.11
- timm

#### Steps

1. 
```bash
git clone https://github.com/Vescrity/PTVDModel.git
```

2. read README of that repo.
3. move the tt.ptl to `TorchVideo/app/src/main/assets/`
4. Get a sample video and move it to `TorchVideo/app/src/main/res/raw/video1.mp4`

### 2. Build with Android Studio

> Tips: 
> Change java version to JAVA8 ( or a version < 13)

`Settings > Build, Execution ... > Build Tools > Gradle > Gradle Projects > Gradle > Gradle JDK`

### 3. Run the app

## TODO

- [x] Basic Functions
- [x] Multiple models
- [x] Config
- [ ] Config Save & Load
- [ ] analyze frequency of Live Mode


---


# LICENSE

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
