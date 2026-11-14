package com.movrit.movrit.chat.service;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 채팅 실시간 알림(Server-Sent Events) 연결을 관리합니다.
 *
 * <ul>
 *   <li>로그인한 화면마다 연결 하나를 열어 두고, 새 메시지·읽음 변화가 생기면 해당 방 참여자에게 짧은 이벤트({roomId, messageId})를 보냅니다.
 *   <li>이벤트에는 메시지 내용을 담지 않습니다. 화면은 이벤트를 받으면 기존 메시지 API로 새 내용을 가져옵니다(권한 확인을 한곳에 둠).
 *   <li>이벤트는 DB 저장이 끝난(커밋) 뒤에 보내 화면이 바로 새 메시지를 읽을 수 있게 합니다.
 *   <li>25초마다 빈 신호를 보내 끊긴 연결을 정리합니다. 단일 서버(로컬 시연) 기준 메모리 보관입니다.
 * </ul>
 */
@Slf4j
@Component
public class ChatEventHub {
  static final long TIMEOUT_MS = 30L * 60 * 1000;

  private record Subscriber(Long memberId, boolean admin, SseEmitter emitter) {}

  private final Map<Long, List<Subscriber>> byMember = new ConcurrentHashMap<>();

  /** 새 연결을 엽니다. 연결이 끝나거나 오류가 나면 목록에서 뺍니다. */
  public SseEmitter subscribe(Long memberId, boolean admin) {
    SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
    Subscriber subscriber = new Subscriber(memberId, admin, emitter);
    byMember.computeIfAbsent(memberId, key -> new CopyOnWriteArrayList<>()).add(subscriber);
    Runnable remove = () -> remove(subscriber);
    emitter.onCompletion(remove);
    emitter.onTimeout(remove);
    emitter.onError(error -> remove.run());
    send(subscriber, "ready", Map.of("ok", true));
    return emitter;
  }

  /** 지정한 회원들과(adminsToo면) 연결된 운영자에게 이벤트를 보냅니다. 트랜잭션 안이면 커밋 뒤에 보냅니다. */
  public void publish(Collection<Long> memberIds, boolean adminsToo, String event, Object data) {
    Runnable task =
        () -> {
          byMember.forEach(
              (memberId, subscribers) ->
                  subscribers.forEach(
                      subscriber -> {
                        if (memberIds.contains(memberId) || (adminsToo && subscriber.admin())) {
                          send(subscriber, event, data);
                        }
                      }));
        };
    afterCommit(task);
  }

  /** 회원의 연결을 모두 닫습니다(이용 정지 등). 트랜잭션 안이면 커밋 뒤에 닫아, 화면이 다시 연결할 때 바뀐 상태로 확인되게 합니다. */
  public void disconnect(Long memberId) {
    Runnable task =
        () -> {
          List<Subscriber> subscribers = byMember.remove(memberId);
          if (subscribers == null) return;
          subscribers.forEach(
              subscriber -> {
                try {
                  subscriber.emitter().complete();
                } catch (IllegalStateException ignored) {
                  // 이미 끝난 연결입니다.
                }
              });
        };
    afterCommit(task);
  }

  /** 끊긴 연결을 찾아내기 위해 25초마다 빈 신호를 보냅니다. */
  @Scheduled(fixedRate = 25_000)
  public void heartbeat() {
    byMember.values().forEach(list -> list.forEach(subscriber -> send(subscriber, "ping", "")));
  }

  /** 트랜잭션 안이면 커밋 뒤에, 아니면 바로 실행합니다. */
  private static void afterCommit(Runnable task) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              task.run();
            }
          });
    } else {
      task.run();
    }
  }

  int connectionCount() {
    return byMember.values().stream().mapToInt(List::size).sum();
  }

  private void send(Subscriber subscriber, String event, Object data) {
    try {
      subscriber.emitter().send(SseEmitter.event().name(event).data(data));
    } catch (IOException | IllegalStateException exception) {
      remove(subscriber);
    }
  }

  private void remove(Subscriber subscriber) {
    List<Subscriber> list = byMember.get(subscriber.memberId());
    if (list != null) {
      list.remove(subscriber);
      if (list.isEmpty()) byMember.remove(subscriber.memberId(), list);
    }
  }
}
