package io.vertx.test.core;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.MockZKCluster;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
public class ZKClusteredAsyncMapTest extends ClusteredAsyncMapTest {

  private MockZKCluster zkClustered = new MockZKCluster();

  public void after() throws Exception {
    super.after();
    zkClustered.stop();
  }

  @Override
  protected ClusterManager getClusterManager() {
    return zkClustered.getClusterManager();
  }

  /**
   * As zookeeper cluster manager using distribution lock, so there is some cost
   * on checking kv in application layer.
   * I have increase delay time that checking value is exist in map.
   * 30ms is ok in my laptop.
   */
  @Override
  @Test
  public void testMapPutTtl() {
    getVertx().sharedData().<String, String>getAsyncMap("foo", onSuccess(map -> {
      map.put("pipo", "molo", 150, onSuccess(vd -> {
        vertx.setTimer(300, l -> {
          getVertx().sharedData().<String, String>getAsyncMap("foo", onSuccess(map2 -> {
            map2.get("pipo", onSuccess(res -> {
              assertNull(res);
              testComplete();
            }));
          }));
        });
      }));
    }));
    await();
  }

  @Override
  @Test
  public void testMapPutIfAbsentTtl() {
    getVertx().sharedData().<String, String>getAsyncMap("foo", onSuccess(map -> {
      map.putIfAbsent("pipo", "molo", 150, onSuccess(vd -> {
        assertNull(vd);
        vertx.setTimer(300, l -> {
          getVertx().sharedData().<String, String>getAsyncMap("foo", onSuccess(map2 -> {
            map2.get("pipo", onSuccess(res -> {
              assertNull(res);
              testComplete();
            }));
          }));
        });
      }));
    }));
    await();
  }

  @Test
  @Override
  @Ignore("This CM removes the binding even if a new entry is added without ttl")
  public void testMapPutTtlThenPut() {
    getVertx().sharedData().getAsyncMap("foo", onSuccess(map -> {
      map.put("pipo", "molo", 150, onSuccess(vd -> {
        map.put("pipo", "mili", onSuccess(vd2 -> {
          vertx.setTimer(300, l -> {
            getVertx().sharedData().getAsyncMap("foo", onSuccess(map2 -> {
              map2.get("pipo", onSuccess(res -> {
                assertEquals("mili", res);
                testComplete();
              }));
            }));
          });
        }));
      }));
    }));
    await();
  }

  @Test
  public void testStoreAndGetBuffer() {
    getVertx().sharedData().<String, Buffer>getAsyncMap("foo", onSuccess(map -> {
      map.put("test", Buffer.buffer().appendString("Hello"), onSuccess(putResult -> map.get("test", onSuccess(myBuffer -> {
        assertEquals("Hello", myBuffer.toString());
        testComplete();
      }))));
    }));
    await();
  }
}
