#include <stdio.h>
#include <unistd.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

// https://rsync.samba.org/tech_report/node3.html
// One single signature:
struct Signature{
	uint32_t a;
	uint32_t b;
};
// List of all signatures with one signature size
struct SignatureList{
	uint32_t signatureSize;
	uint32_t numSignatures;
	struct Signature* signatures;
};
// Algorithm data for rollign signature calculation
struct RollingSignatureCalculator{
	uint32_t signatureSize;
	uint32_t *buf;
	uint32_t bufPos;
	uint32_t a;
	uint32_t b;
	uint32_t initialized;
};
// List of all signatures
struct SignatureListArray{
	uint32_t numSignatureSizes;
	struct SignatureList* lists;
};
void usage();
void rollingSignatureCalculatorInit(struct RollingSignatureCalculator* s, uint32_t signatureSize);
void rollingSignatureAdd(struct RollingSignatureCalculator* s, uint32_t val);
void filterRollingArm(void* ptr, size_t len);
void filterRollingAarch64v1(void* ptr, size_t len);

enum ARCHITECTURE{
	ARM,
	THUMB,
	AARCH64V1
};
int main(int argc, char** argv){
	uint32_t tmp[4];
	struct SignatureListArray signatures;
	struct RollingSignatureCalculator *signatureCalculators;
	uint32_t i,j,k;
	int argPos;
	FILE* f;
	char* buf;
	uint32_t bytesRead;
	if(argc < 3){
		usage();
	}
	// argv[1]: mode (--arm, --aarch64v1, --thumb)
	enum ARCHITECTURE arch;
	if(strcmp(argv[1], "--arm") == 0){
		arch = ARM;
	} else if(strcmp(argv[1], "--thumb") == 0){
		arch = THUMB;
	} else if(strcmp(argv[1], "--aarch64v1") == 0){
		arch = AARCH64V1;
	} else{
		usage();
	}
	// argv[2]: Command
	struct RollingSignatureCalculator signatureCalculator;
	uint32_t startPos, len;
	if(strcmp(argv[2],"calc") == 0){
		argPos = 4;
		while(argPos < argc){
			startPos = atoi(argv[argPos]);
			len = atoi(argv[argPos+1]);
			//fprintf(stderr, "START: %d  LEN=%d\n", startPos, len);
			if(len > 1024*1024){
				fprintf(stderr, "Too long: %d\n", len);
				exit(1);
			}
			if(len % 4 != 0){
				fprintf(stderr, "Length must be a multiple of 4 bytes\n");
				exit(1);
			}
			rollingSignatureCalculatorInit(&signatureCalculator, len/4);
			f = fopen(argv[3],"r");
			fseek(f, startPos, SEEK_SET);
			if(ftell(f) != startPos){
				fprintf(stderr, "Seek failed\n");
				exit(1);
			}
			buf = malloc(len);
			bytesRead = fread(buf, 1, len, f);
			if(bytesRead != len){
				fprintf(stderr,"Short read: %d instead of %d at pos %d\n", bytesRead, len, startPos);
				exit(2);
			}
			fclose(f);
			if(arch == ARM){
				filterRollingArm(buf, len);
			} else if(arch == AARCH64V1){
				filterRollingAarch64v1(buf, len);
			}
			for(i=0;i<len;i+=4){
				rollingSignatureAdd(&signatureCalculator, *((uint32_t*)(buf+i)));
				//fprintf(stderr, "SIG: 0x%x 0x%x\n", signatureCalculator.a, signatureCalculator.b);
			}
			//fprintf(stderr, "SIG: 0x%x 0x%x\n", signatureCalculator.a, signatureCalculator.b);
			fwrite(&signatureCalculator.a, 4, 1, stdout);
			fwrite(&signatureCalculator.b, 4, 1, stdout);
			argPos += 2;
		}
	} else if(strcmp(argv[2], "search") == 0){
		// Read in signatures from stdin
		if(fread(tmp, 4, 1, stdin) != 1){
			fprintf(stderr, "Short read from stdin\n");
			exit(1);
		}
		signatures.numSignatureSizes = tmp[0];
		if(signatures.numSignatureSizes > 1024){
			fprintf(stderr, "Too many signature sizes: %d\n", signatures.numSignatureSizes);
			exit(1);
		}
		signatures.lists = calloc(signatures.numSignatureSizes, sizeof(struct SignatureList));
		if(signatures.lists == NULL){
			fprintf(stderr, "calloc failed\n");
			exit(1);
		}
		signatureCalculators = calloc(signatures.numSignatureSizes, sizeof(struct RollingSignatureCalculator));
		if(signatureCalculators == NULL){
			fprintf(stderr, "calloc failed\n");
			exit(1);
		}
		for(i=0;i<signatures.numSignatureSizes;i++){
			// Read checksums to search for from stdin
			// Input format: 4 bytes count, 4 bytes rolling checksum len (or 0 for last one), some 64-bit checksums
			if(fread(tmp, 4, 2, stdin) != 2){
				fprintf(stderr, "Short read from stdin\n");
				exit(1);
			}
			signatures.lists[i].numSignatures = tmp[0];
			if(tmp[1] % 4 != 0){
				fprintf(stderr, "Invalid signature size %u\n", tmp[1]);
			}
			signatures.lists[i].signatureSize = tmp[1]/4;
			if(signatures.lists[i].numSignatures > 1024*1024){
				fprintf(stderr, "Too many signatures: %d\n", signatures.lists[i].numSignatures);
				exit(1);
			}
			if(signatures.lists[i].signatureSize > 1024*1024){
				fprintf(stderr, "Signature size too big: %d\n", signatures.lists[i].signatureSize);
				exit(1);
			}
			rollingSignatureCalculatorInit(&signatureCalculators[i], signatures.lists[i].signatureSize);
			signatures.lists[i].signatures = calloc(signatures.lists[i].numSignatures, sizeof(struct Signature));
			if(signatures.lists[i].signatures == NULL){
				fprintf(stderr, "calloc failed\n");
				exit(1);
			}
			for(j=0;j<signatures.lists[i].numSignatures;j++){
				if(fread(tmp, 4, 2, stdin) != 2){
					fprintf(stderr, "Short read from stdin\n");
					exit(1);
				}
				signatures.lists[i].signatures[j].a = tmp[0];
				signatures.lists[i].signatures[j].b = tmp[1];
			}
		}
		f = fopen(argv[3], "r");
		if(f == NULL){
			perror("Failed to open file\n");
			exit(1);
		}
		buf = malloc(1024*1024);
		while(1){
			bytesRead = fread(buf, 1, 1024*1024, f);
			if(bytesRead == 0){
				break;
			}
			if(arch == ARM){
				filterRollingArm(buf, bytesRead);
			} else if(arch == AARCH64V1){
				filterRollingAarch64v1(buf, bytesRead);
			}
			for(i = 0;i < bytesRead; i+=4){
				for(j=0;j<signatures.numSignatureSizes;j++){
					rollingSignatureAdd(&signatureCalculators[j], *((uint32_t*)(buf+i)));
					for(k=0;k<signatures.lists[j].numSignatures;k++){
						//printf("WANTED: a=0x%08x b=0x%08x\n", signatures.lists[j].signatures[k].a, signatures.lists[j].signatures[k].b);
						if(signatures.lists[j].signatures[k].a == signatureCalculators[j].a && signatures.lists[j].signatures[k].b == signatureCalculators[j].b){
							// fprintf(stderr, "Found match at 0x%08x siglen %d: a=0x%08x b=0x%08x\n", i + 4 - 4*signatureCalculators[j].signatureSize, 4*signatures.lists[j].signatureSize, signatureCalculators[j].a, signatureCalculators[j].b);
							tmp[0] = i + 4 - 4*signatureCalculators[j].signatureSize;
							tmp[1] = 4*signatureCalculators[j].signatureSize;
							tmp[2] = signatureCalculators[j].a;
							tmp[3] = signatureCalculators[j].b;
							fwrite(tmp, 4, 4, stdout);
						} else{
							//printf("No match at %d siglen %d: a=0x%08x b=0x%08x\n", i, signatures.lists[j].signatureSize, signatureCalculators[j].a, signatureCalculators[j].b);
						}
					}
				}
			}
		}
	} else if(strcmp(argv[2], "filter") == 0){
		while(1){
			buf = malloc(4);
			bytesRead = fread(buf,1,4,stdin);
			if(bytesRead == 0){
				exit(0);
			} else if(bytesRead != 4){
				fprintf(stderr, "fread(buf,1,4,stdin) returned %d\n", bytesRead);
				exit(1);
			}
			if(arch == ARM){
				filterRollingArm(buf, 4);
			} else if(arch == AARCH64V1){
				filterRollingAarch64v1(buf, 4);
			}
			fwrite(buf,1,4,stdout);
		}
	}
}

void usage(){
	fprintf(stderr, "USAGE: TODO\n");
	exit(1);
}

void rollingSignatureCalculatorInit(struct RollingSignatureCalculator* s, uint32_t signatureSize){
	int i;
	s->a = 0;
	s->b = 0;
	s->signatureSize = signatureSize;
	s->initialized = 0;
	s->bufPos = 0;
	s->buf = calloc(signatureSize, 4);
	for(i=0;i<signatureSize;i++){
		s->buf[i] = 0;
	}
}
void rollingSignatureAdd(struct RollingSignatureCalculator* s, uint32_t val){
	uint32_t oldElement = s->buf[s->bufPos+1];
	s->a = s->a - oldElement + val;
	s->b = s->b - s->signatureSize * oldElement + s->a;
	s->buf[s->bufPos+1] = val;
	s->bufPos = (s->bufPos + 1) % s->signatureSize;
}


// In-place filters instructions for rolling checksum analysis, no knowledge of start/end of functions needed
// http://imrannazar.com/ARM-Opcode-Map
void filterRollingArm(void* ptr, size_t len){
	exit(1); // Old, incomplete code, Currently only AARCH64 is really supported.
	void* endPtr = ptr + len;
	uint32_t inst;
	uint32_t condition, bits2724, bits2720, bits0704, Rn, Rd;
	while(ptr < endPtr){
		inst = *((uint32_t*)ptr);
		condition = (inst & 0xf0000000) >> 28;
		bits2724 = (inst & 0x0f000000) >> 24; // First byte, lower half, Good for getting generic instruction type
		bits2720 = (inst & 0x0ff00000) >> 20;
		//bits0704 = (inst & 0x000000f0) >> 4;
		Rn = (inst & 0x000f0000)  >> 16;
		// Rd = (inst & 0x0000f000)  >> 12;
		if(bits2724 == 0xb){ // BL instruction
			*((uint32_t*)ptr) = inst & 0xff000000; // Clear full call destination
		} else if(condition == 0xf && bits2724 == 0xa){ // BLX instruction
			*((uint32_t*)ptr) = inst & 0xff000000; // Clear full call destination
		} else if((bits2720 == 0x59 || bits2720 == 0x51) && Rn == 0xf){ // PC-relative LDR, Sample: e59fe004 ldr lr, [pc, #4]
			*((uint32_t*)ptr) = inst & 0xfffff000; // Clear offset, Maybe not needed
		} else if( (bits2720 == 0xd9 || bits2720 == 0xdd) && Rn == 0xf){ // PC-relative VLDR, Sample: vldr s0, [pc, #572]
			*((uint32_t*)ptr) = inst & 0xffffff00; // Clear offset, Maybe not needed
		} else if(bits2724 == 0xa){ // B instruction
			// If it is an unconditional branch, it may actually be a call to another function optimized ot a jump by GCC.
			// Since we don't know the function start/end address in rolling checksum mode, let's just clear the destination for all unconditional branch instructions.
			if(condition == 0xe){
				*((uint32_t*)ptr) = inst & 0xff000000;
			}
			// Conditional branch instructions should always be within the current function.
		}
		ptr += 4;
	}
}
// http://kitoslab-eng.blogspot.de/2012/10/armv8-aarch64-instruction-encoding.html
void filterRollingAarch64v1(void* ptr, size_t len){
	void* endPtr = ptr + len;
	uint32_t inst;
	uint32_t condition, bits2724, bits2720, bits0704, Rn, Rd;
	while(ptr < endPtr){
		inst = *((uint32_t*)ptr);
		if( (inst & 0x3f000000) == 0x1c000000){ // xx01 1100 iiii iiii iiii iiii iiit tttt  -  ldr Ft ADDR_PCREL19
			*((uint32_t*)ptr) = inst & 0xff00001f;
		} else if( (inst & 0xbf000000) == 0x18000000){ // 0x01 1000 iiii iiii iiii iiii iiit tttt  -  ldr Rt ADDR_PCREL19
			*((uint32_t*)ptr) = inst & 0xff00001f;
		} else if( (inst & 0xff000000) == 0x98000000){ // 1001 1000 iiii iiii iiii iiii iiit tttt  -  ldrsw Rt ADDR_PCREL1
			*((uint32_t*)ptr) = inst & 0xff00001f;
		} else if( (inst & 0x8f000000) == 0x00000000){ // 0iix 0000 iiii iiii iiii iiii iiid dddd  -  adr Rd ADDR_PCREL21
			*((uint32_t*)ptr) = inst & 0x9f00001f;
		} else if( (inst & 0xec000000) == 0x04000000){ // 000x 01ii iiii iiii iiii iiii iiii iiii  -  b ADDR_PCREL26
			*((uint32_t*)ptr) = inst & 0xfc000000;
		} else if( (inst & 0xec000000) == 0x84000000){ // 100x 01ii iiii iiii iiii iiii iiii iiii  -  bl ADDR_PCREL26
			*((uint32_t*)ptr) = inst & 0xfc000000;
		} else if((inst & 0x61000000) == 0x01000000){ // x00x 0001 SSii iiii iiii iinn nnnd dddd  -  add Rd_SP Rn_SP AIMM
			// Problem: Code often needs to get pc-relative addresses to nearby code (e.g. for c++ tables). This is typically implemented with ADRP Rx, pcrel and ADD Rx, Rx, #0x...
			// Sample code:
			//  17aee0:       90000008        adrp    x8, 17a000 <_ZN11SkJpegCodec18initializeSwizzlerERK11SkImageInfoRKN7SkCodec7OptionsE+0x54>
			//	17aee4:       f9001c01        str     x1, [x0,#56]
			//	17aee8:       913c8108        add     x8, x8, #0xf20
			*((uint32_t*)ptr) = inst & 0xff0003ff;
			/*
			 * This doesn't work reliably enough, sometimes the compiler is generating relocation (R_AARCH64_ADD_ABS_LO12_NC) add instructions with two different registers
			 if( (inst & 0x1f) == ( (inst & 0x3e0) >> 5) ){ // Rd == Rn
				*((uint32_t*)ptr) = inst & 0xff0003ff;
			}
			* */
		} else if( (inst & 0x8f000000) == 0x80000000){ // 1iix 0000 iiii iiii iiii iiii iiid dddd  -  adrp Rd ADDR_ADRP
			*((uint32_t*)ptr) = inst & 0x9f00001f;
		} else if( (inst & 0xffc00000) == 0x3dc00000){ // xxx1 1101 x1ii iiii iiii iinn nnnt tttt  -  ldr Ft ADDR_UIMM12
			// Sample code:
			// 418cac:       90000d68        adrp    x8, 5c4000 <VP8kabs0+0xf7c0>
			// 418cb0:       b0000769        adrp    x9, 505000 <_Unwind_Find_FDE+0x67cac>
			// 418cb4:       f9442908        ldr     x8, [x8,#2128]
			// 418cb8:       3dc0b920        ldr     q0, [x9,#736] <======================================
			*((uint32_t*)ptr) = inst & 0xffc00000;
		} else if( (inst & 0x9fc00000) == 0x99400000){ // 1xx1 1001 01ii iiii iiii iinn nnnt tttt  -  ldr Rt ADDR_UIMM12
			// Sample code:
			// 418cac:       90000d68        adrp    x8, 5c4000 <VP8kabs0+0xf7c0>
			// 418cb0:       b0000769        adrp    x9, 505000 <_Unwind_Find_FDE+0x67cac>
			// 418cb4:       f9442908        ldr     x8, [x8,#2128] <======================================
			// 418cb8:       3dc0b920        ldr     q0, [x9,#736]
			*((uint32_t*)ptr) = inst & 0xffc00000;
		} else if( (inst & 0x1f400000) == 0x1d400000){ // xxx1 1101 x1ii iiii iiii iinn nnnt tttt  -  ldr Ft ADDR_UIMM12
			// Sample code:
			// 413038:       d0000788        adrp    x8, 505000 <_Unwind_Find_FDE+0x67eec>
			// 41303c:       bd404000        ldr     s0, [x0,#64]
			// 413040:       bd46d901        ldr     s1, [x8,#1752] <======================================
			// Other sample code: 25e4b4:       fd475902        ldr     d2, [x8,#3760] => The X (don't care) bits are not always the same
			*((uint32_t*)ptr) = inst & 0xffc00000;
		} else if ( (inst & 0x6f000000) == 0x01000000){ // x00x 0001 SSii iiii iiii iinn nnnd dddd  -  add Rd_SP Rn_SP AIMM
			// add instruction frequently used in combination with adrp for relocations
			*((uint32_t*)ptr) = inst & 0xffc003ff;
		}
		// Ignore b.c, cbz, cbnz, tbz, tbnz since conditional jumps are typically within the function
		// Ignore prfm since prefetch instructions are probably not used
		ptr += 4;
	}
}
