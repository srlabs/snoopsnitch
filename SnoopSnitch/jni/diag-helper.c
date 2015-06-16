#include <sys/types.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <sys/un.h>

#include <android/log.h>

#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>


#define BUF_SIZE 1000000
static char from_dev_buf[BUF_SIZE];
static char from_sock_buf[BUF_SIZE];

static int to_app_fd;
static int from_app_fd;
static int diag_fd;


__attribute__((__format__(__printf__, 2, 3)))
static void
logmsg(int prio, const char *fmt, ...)
{
        va_list args;

        va_start(args, fmt);
        __android_log_vprint(prio, "diag-helper", fmt, args);
        va_end(args);
}

static void
loghex(const char *desc, char *buf, size_t len)
{
        char *hexbuf = calloc(1, len * 3 + 1);

        for (int i = 0; i < len; ++i)
                sprintf(&hexbuf[i * 3], " %02x", buf[i]);
        logmsg(ANDROID_LOG_DEBUG, "%s:%s", desc, hexbuf);
        free(hexbuf);
}

static int
open_diag_dev(void)
{
        int diag_fd = -1;
        int rv = -1;
        int olderrno;

        logmsg(ANDROID_LOG_DEBUG, "opening diag device");

        diag_fd = open("/dev/diag", O_RDWR|O_CLOEXEC);
        if (diag_fd < 0) {
                logmsg(ANDROID_LOG_FATAL, "error opening diag device: %m");
                goto exit;
        }

        const unsigned long DIAG_IOCTL_SWITCH_LOGGING = 7;
        const int MEMORY_DEVICE_MODE = 2;

        //  In commit ae92f0b2 of the MSM kernel this ioctl was changed to
        //  have its parameter passed as a pointer. I don't know how to detect
        //  that reliably, so I brute-force the right method.
        rv = ioctl(diag_fd, DIAG_IOCTL_SWITCH_LOGGING, MEMORY_DEVICE_MODE);
        if (rv < 0) {
            olderrno = errno;
            rv = ioctl(diag_fd, DIAG_IOCTL_SWITCH_LOGGING, (void *)&MEMORY_DEVICE_MODE);
        }

        if (rv < 0) {
            logmsg(ANDROID_LOG_FATAL, "error setting diag device logging mode: %s/%s",
				strerror(olderrno), strerror(errno));
            close(diag_fd);
            diag_fd = -1;
            goto exit;
        }

        logmsg(ANDROID_LOG_DEBUG, "diag device opened");

exit:
        return (diag_fd);
}


static int
data_pump_dev(void)
{
        for (;;) {
                ssize_t read_len = read(diag_fd, from_dev_buf, BUF_SIZE);
                if (read_len == 0) {
                        /**
                         * Empty reads happens when we get interrupted,
                         * e.g. on strace attach.
                         */
                        continue;
                }
                if (read_len == -1) {
                        logmsg(ANDROID_LOG_ERROR, "cannot read from device: %s", strerror(errno));
                        return (-1);
                }
                if (read_len == BUF_SIZE) {
                        logmsg(ANDROID_LOG_WARN, "short read from device");
                }

                /* loghex("data from device", from_dev_buf, read_len); */

                int short_write = 0;

                uint32_t be_len = htonl(read_len);
                ssize_t write_len = write(to_app_fd, &be_len, sizeof(be_len));

                if (write_len != sizeof(be_len)) {
                        /* failed to write length */
                        if (write_len != -1)
                                short_write = 1;
                } else {
                        /* success, now write data */
                        write_len = write(to_app_fd, from_dev_buf, read_len);
                        if (write_len > 0 && write_len != read_len)
                                short_write = 1;
                }

                if ((write_len == -1 && errno == EPIPE) || short_write) {
                        logmsg(ANDROID_LOG_INFO, "app write side closed the connection");
                        return (0);
                }
                if (write_len == -1) {
                        logmsg(ANDROID_LOG_ERROR, "cannot write message to app: %s", strerror(errno));
                        return (-1);
                }
        }
}

static void *
data_pump_app(void *arg)
{
        for (;;) {
                int short_read = 0;

                uint32_t be_len;
                ssize_t read_len = read(from_app_fd, &be_len, sizeof(be_len));
		// Retry once in case of a short read
		if(read_len > 0 && read_len != sizeof(be_len)){
		        read_len += read(from_app_fd, ((char*)&be_len)+read_len,sizeof(be_len)-read_len);
		}

                if (read_len != sizeof(be_len)) {
		        short_read = 1;
                } else {
                        /* success, now read data */
                        uint32_t data_len = ntohl(be_len);
                        if (data_len > BUF_SIZE) {
                                logmsg(ANDROID_LOG_ERROR, "data length %d exceeds buffer", data_len);
                                goto error;
                        }
			size_t tmp;
			read_len = 0;
			// Retry
			while(read_len < data_len){
			        tmp = read(from_app_fd, from_sock_buf, data_len);
				if(tmp == -1){
				       logmsg(ANDROID_LOG_ERROR, "cannot read from app: %s", strerror(errno));
				       goto error;
				}
				read_len += tmp;
				if(tmp == 0)
				       break;
			}
                        if (read_len > 0 && read_len != data_len)
                                short_read = 1;
                }
                if (short_read) {
			logmsg(ANDROID_LOG_INFO, "app read side closed the connection");
			goto exit;
                }

                /* loghex("data from app", from_sock_buf, read_len); */

                ssize_t write_len = write(diag_fd, from_sock_buf, read_len);
                if (write_len == -1) {
                        logmsg(ANDROID_LOG_ERROR, "cannot write to device: %s", strerror(errno));
                        goto error;
                }
        }
exit:
        exit(0);
error:
        exit(12);
}


int
main(int argc, char **argv)
{
        int for_real = 0;
        int rv = -1;

        logmsg(ANDROID_LOG_INFO, "starting");

        if (argc != 2) {
                logmsg(ANDROID_LOG_ERROR, "not invoked with enough arguments");
                exit(15);
        }

        if (strcmp(argv[1], "run") == 0) {
                for_real = 1;
        } else if (strcmp(argv[1], "test") == 0) {
                logmsg(ANDROID_LOG_INFO, "test mode invoked");
                for_real = 0;
        } else {
                logmsg(ANDROID_LOG_ERROR, "invalid run mode `%s': chose `run' or `test'", argv[1]);
                exit(15);
        }

        signal(SIGPIPE, SIG_IGN);

        from_app_fd = dup(STDIN_FILENO);
        to_app_fd = dup(STDOUT_FILENO);

        if (from_app_fd < 0 || to_app_fd < 0) {
                logmsg(ANDROID_LOG_FATAL, "failed to prepare communication descriptors");
                exit(14);
        }

        /* We can't daemonize, because su then closes the stdin/out connection */

        /**
         * We can only open the device after we daemonize, because the
         * kernel driver is tracking us by pid.
         */
        diag_fd = open_diag_dev();
        if (diag_fd < 0) {
                logmsg(ANDROID_LOG_ERROR, "error opening DIAG device");
                exit(10);
        }

        if (write(to_app_fd, "OKAY", 4) != 4) {
                logmsg(ANDROID_LOG_ERROR, "failed to write handshake message");
                exit(16);
        }

        /* Return success if we are only testing */
        if (!for_real) {
                logmsg(ANDROID_LOG_INFO, "test successful");
                return (0);
        }

        /* XXX drop permissions */
        //setreuid

        static pthread_t from_sock_thread;
        int res = pthread_create(&from_sock_thread, NULL, data_pump_app, NULL);
        if (res != 0) {
                logmsg(ANDROID_LOG_FATAL, "failed to spawn socket data pump thread: %s", strerror(res));
                exit(11);
        }
        rv = data_pump_dev();
        if (rv < 0) {
            exit(1);
        }

        logmsg(ANDROID_LOG_DEBUG, "closing socket connection");

        exit(0);
}
